#!/usr/bin/env python3
"""Génère les scripts SQL de seed pour care_plans/care_plan_tasks (DB
caresync_careplan) et medicaments/prescriptions/prescription_lines (DB
caresync_prescription), en s'appuyant sur les patients déjà persistés dans
caresync_patient (cf. generate_seed.py, à exécuter avant celui-ci).

Usage :
  docker exec caresync-postgres psql -U caresyncadmin -d caresync_patient \
    -t -A -F'|' -c "SELECT id, pathology, assignedmedicid FROM patients;" \
    > patients_export.psv
  python3 generate_dossier_seed.py
"""
import random
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path

random.seed(42)
now = datetime.now(timezone.utc)
outdir = Path(__file__).parent

def uid():
    return str(uuid.uuid4())

def esc(s):
    return s.replace("'", "''") if s else s

def iso(dt):
    return dt.strftime("%Y-%m-%d %H:%M:%S+00")

def date_only(dt):
    return dt.strftime("%Y-%m-%d")

# ── Patients existants (id, pathology, assignedmedicid) ──────────────────────
patients = []
with open(outdir / "patients_export.psv") as f:
    for line in f:
        pid, patho, medic = line.rstrip("\n").split("|")
        patients.append({"id": pid, "pathology": patho, "medicId": medic})

# ── Mapping pathologie -> typePathologie backend + tâches (cf.
#    CarePlanResource.java, mêmes valeurs que celles auto-générées par POST /) ─
PATHO_TO_TYPE = {"Diabète T2": "DIABETE_T2", "Insuffisance cardiaque": "ICFE", "BPCO": "BPCO"}
TASKS_BY_TYPE = {
    "DIABETE_T2": ["mesure_glycemie_quotidienne", "consultation_mensuelle", "HbA1c_trimestriel"],
    "ICFE":       ["peser_quotidien", "spo2_quotidien", "consultation_bimensuelle"],
    "BPCO":       ["spirometrie_hebdomadaire", "spo2_quotidien", "consultation_mensuelle"],
}
OBJECTIFS = {
    "DIABETE_T2": "HbA1c < 7% et prévention des complications micro/macro-vasculaires",
    "ICFE":       "Stabilisation du poids sec et réduction des ré-hospitalisations",
    "BPCO":       "Maintien SpO2 > 92% et réduction des exacerbations",
}

# ── Référentiel Médicaments (10, réels et pertinents aux 3 pathologies) ──────
MEDICAMENTS = [
    # codeATC, codeCIS, DCI, nomCommercial, dosage, forme, voie, pathologie cible
    ("A10BA02", "63455923", "Metformine",  "Glucophage",  "1000mg", "Comprimé pelliculé", "Orale", "DIABETE_T2"),
    ("A10AB01", "60234112", "Insuline humaine rapide", "Actrapid", "100UI/mL", "Solution injectable", "Sous-cutanée", "DIABETE_T2"),
    ("A10BJ02", "68321445", "Liraglutide", "Victoza", "6mg/mL", "Solution injectable", "Sous-cutanée", "DIABETE_T2"),
    ("C07AB07", "34009301", "Bisoprolol",  "Cardensiel",  "5mg",   "Comprimé pelliculé", "Orale", "ICFE"),
    ("C03CA01", "34009278", "Furosémide",  "Lasilix",     "40mg",  "Comprimé sécable",   "Orale", "ICFE"),
    ("C09AA05", "34009156", "Ramipril",    "Triatec",     "5mg",   "Gélule",             "Orale", "ICFE"),
    ("C03DA04", "68932011", "Éplérénone",  "Inspra",      "25mg",  "Comprimé pelliculé", "Orale", "ICFE"),
    ("R03AC02", "34009445", "Salbutamol",  "Ventoline",   "100µg/dose", "Aérosol-doseur", "Inhalée", "BPCO"),
    ("R03BB04", "63234891", "Tiotropium",  "Spiriva",     "18µg",  "Poudre pour inhalation", "Inhalée", "BPCO"),
    ("R03AK07", "69234512", "Formotérol/Budésonide", "Symbicort", "200/6µg/dose", "Aérosol-doseur", "Inhalée", "BPCO"),
]

# ── 1. Médicaments ────────────────────────────────────────────────────────────
med_sql = []
med_ids = {}
for atc, cis, dci, nom, dosage, forme, voie, patho_type in MEDICAMENTS:
    mid = uid()
    med_ids.setdefault(patho_type, []).append(mid)
    med_sql.append(
        f"INSERT INTO medicaments (id, codeatc, codecis, denominationcommune, nomcommercial, dosage, "
        f"formegalenique, voieadministration, interactions, classesinteragissantes, actif) VALUES "
        f"('{mid}', '{atc}', '{cis}', '{esc(dci)}', '{esc(nom)}', '{dosage}', '{esc(forme)}', '{voie}', "
        f"false, NULL, true);"
    )

# ── 2. Care plans + tasks (1 plan ACTIF par patient, tâches associées) ───────
careplan_sql = []
for p in patients:
    ptype = PATHO_TO_TYPE.get(p["pathology"])
    if not ptype:
        continue
    plan_id = uid()
    created = now - timedelta(days=random.randint(5, 180))
    careplan_sql.append(
        f"INSERT INTO care_plans (id, patientid, typepathologie, objectif, statut, medicid, createdat) VALUES "
        f"('{plan_id}', '{p['id']}', '{ptype}', '{esc(OBJECTIFS[ptype])}', 'ACTIF', '{p['medicId']}', '{iso(created)}');"
    )
    for task_type in TASKS_BY_TYPE[ptype]:
        task_id = uid()
        statut = random.choices(["EN_ATTENTE", "EN_COURS", "REALISEE"], weights=[40, 20, 40])[0]
        echeance = now + timedelta(days=random.randint(-3, 14))
        realisee_sql = f"'{iso(now - timedelta(days=random.randint(0,5)))}'" if statut == "REALISEE" else "NULL"
        careplan_sql.append(
            f"INSERT INTO care_plan_tasks (id, careplanid, typetask, statut, echeanceat, realiseeat, valeur, commentaire, createdat) VALUES "
            f"('{task_id}', '{plan_id}', '{task_type}', '{statut}', '{iso(echeance)}', {realisee_sql}, NULL, NULL, '{iso(created)}');"
        )

# ── 3. Prescriptions + lignes (≈80% des patients, 1-3 lignes cohérentes) ────
rx_sql = []
for p in patients:
    if random.random() > 0.80:
        continue
    ptype = PATHO_TO_TYPE.get(p["pathology"])
    candidates = med_ids.get(ptype, [])
    if not candidates:
        continue
    rx_id = uid()
    emission = now - timedelta(days=random.randint(0, 60))
    expiration = emission + timedelta(days=90)
    statut = random.choices(["SIGNEE", "DELIVREE"], weights=[35, 65])[0]
    rx_sql.append(
        f"INSERT INTO prescriptions (id, patientid, prescripteurid, statut, dateemission, dateexpiration, "
        f"signatureelectronique, signedat, certificatrpps, renouvellementpossible, nombrerenouvellements, "
        f"prescriptionparentid, createdat) VALUES "
        f"('{rx_id}', '{p['id']}', '{p['medicId']}', '{statut}', '{date_only(emission)}', '{date_only(expiration)}', "
        f"'demo-signature-base64', '{iso(emission)}', 'RPPS-DEMO', true, 2, NULL, '{iso(emission)}');"
    )
    n_lines = random.randint(1, min(3, len(candidates)))
    for medicament_id in random.sample(candidates, n_lines):
        line_id = uid()
        posologie = random.choice([
            "1 comprimé matin et soir", "1 comprimé le matin", "2 bouffées x3/jour si besoin",
            "1 injection le soir", "1 gélule au coucher",
        ])
        rx_sql.append(
            f"INSERT INTO prescription_lines (id, prescriptionid, medicamentid, posologie, dureejours, "
            f"voiespecifique, instructionsspeciales, alerteinteraction, interactionconfirmee) VALUES "
            f"('{line_id}', '{rx_id}', '{medicament_id}', '{esc(posologie)}', 90, NULL, NULL, NULL, false);"
        )

with open(outdir / "04_careplan.sql", "w") as f:
    f.write("\n".join(careplan_sql) + "\n")

with open(outdir / "05_prescription.sql", "w") as f:
    f.write("\n".join(med_sql + rx_sql) + "\n")

n_plans = sum(1 for l in careplan_sql if l.startswith("INSERT INTO care_plans"))
n_tasks = sum(1 for l in careplan_sql if l.startswith("INSERT INTO care_plan_tasks"))
n_rx = sum(1 for l in rx_sql if l.startswith("INSERT INTO prescriptions"))
n_lines = sum(1 for l in rx_sql if l.startswith("INSERT INTO prescription_lines"))
print(f"Médicaments: {len(med_sql)}")
print(f"Plans de soins: {n_plans} | Tâches: {n_tasks}")
print(f"Prescriptions: {n_rx} | Lignes: {n_lines}")
