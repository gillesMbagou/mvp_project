#!/usr/bin/env python3
"""Génère les scripts SQL de seed pour les 4 bases CareSync (colonnes lowercase, Hibernate)."""
import random
import uuid
from datetime import datetime, timedelta, timezone

random.seed(42)

PRENOMS_H = ["Jean","Pierre","Michel","Alain","Philippe","Bernard","Daniel","Claude","André","Marcel",
             "Thomas","Nicolas","Julien","Antoine","Vincent","Mehdi","Karim","Yanis","Louis","Hugo"]
PRENOMS_F = ["Marie","Nathalie","Isabelle","Sylvie","Catherine","Françoise","Monique","Christine","Sophie","Anne",
             "Camille","Julie","Léa","Emma","Chloé","Fatima","Amel","Sarah","Laura","Clara"]
NOMS = ["Martin","Bernard","Dubois","Thomas","Robert","Petit","Durand","Leroy","Moreau","Simon",
        "Laurent","Lefebvre","Michel","Garcia","David","Bertrand","Roux","Vincent","Fournier","Morel",
        "Girard","André","Lefèvre","Mercier","Dupont","Lambert","Bonnet","François","Martinez","Legrand",
        "Garnier","Faure","Rousseau","Blanc","Guerin","Muller","Henry","Roussel","Nicolas","Perrin"]

ETABLISSEMENTS = [
    ("CHU Saint-Luc",        "CHU",             "751120018"),
    ("Clinique du Parc",     "CLINIQUE_PRIVEE", "751120019"),
    ("Cabinet Médical Alsace", "CABINET",        "751120020"),
]

PATHOLOGIES = ["Diabète T2", "Insuffisance cardiaque", "BPCO"]
SPECIALITES = {
    "Diabète T2": "Endocrinologue",
    "Insuffisance cardiaque": "Cardiologue",
    "BPCO": "Pneumologue",
}
SERVICES = {
    "Diabète T2": "Diabétologie",
    "Insuffisance cardiaque": "Cardiologie",
    "BPCO": "Pneumologie",
}

def esc(s):
    return s.replace("'", "''") if s else s

def uid():
    return str(uuid.uuid4())

def rand_name(sexe=None):
    sexe = sexe or random.choice(["H", "F"])
    prenom = random.choice(PRENOMS_H if sexe == "H" else PRENOMS_F)
    nom = random.choice(NOMS)
    return prenom, nom, sexe

def iso(dt):
    return dt.strftime("%Y-%m-%d %H:%M:%S+00")

now = datetime.now(timezone.utc)

# ── 1. Établissements ────────────────────────────────────────────────────────
etab_sql = []
etab_ids = []
for nom, typ, finess in ETABLISSEMENTS:
    eid = uid()
    etab_ids.append(eid)
    etab_sql.append(
        f"INSERT INTO etablissements (id, nom, finess, type, statut, adresse, codepostal, ville, pays, "
        f"telephone, emailcontact, siteweb, keycloakgroupid, createdat, updatedat) VALUES "
        f"('{eid}', '{esc(nom)}', '{finess}', '{typ}', 'ACTIF', '12 rue de la Santé', '75013', 'Paris', 'France', "
        f"'0140000000', 'contact@{finess}.caresync.be', 'https://{finess}.caresync.be', NULL, "
        f"'{iso(now)}', '{iso(now)}');"
    )

# ── 2. Professionnels : 20 médecins + 50 infirmiers ──────────────────────────
prof_sql = []
medecin_ids = []
used_emails = set()
used_rpps = set()

def make_professionnel(role, service, specialite=None):
    prenom, nom, _ = rand_name()
    pid = uid()
    email = f"{prenom.lower()}.{nom.lower()}@caresync.be"
    n = 1
    base_email = email
    while email in used_emails:
        email = base_email.replace("@", f"{n}@")
        n += 1
    used_emails.add(email)
    rpps = None
    if role == "MEDECIN":
        rpps = str(random.randint(10000000000, 10999999999))
        while rpps in used_rpps:
            rpps = str(random.randint(10000000000, 10999999999))
        used_rpps.add(rpps)
    etab = random.choice(etab_ids)
    kc_uid = uid()
    spec_sql = f"'{esc(specialite)}'" if specialite else "NULL"
    rpps_sql = f"'{rpps}'" if rpps else "NULL"
    prof_sql.append(
        f"INSERT INTO professionnels (id, etablissementid, keycloakuserid, prenom, nom, email, telephone, rpps, "
        f"role, service, specialite, statut, createdat, updatedat) VALUES "
        f"('{pid}', '{etab}', '{kc_uid}', '{esc(prenom)}', '{esc(nom)}', '{email}', "
        f"'06{random.randint(10000000,99999999)}', {rpps_sql}, '{role}', '{esc(service)}', {spec_sql}, 'ACTIF', "
        f"'{iso(now)}', '{iso(now)}');"
    )
    return pid

for _ in range(20):
    patho = random.choice(PATHOLOGIES)
    mid = make_professionnel("MEDECIN", SERVICES[patho], SPECIALITES[patho])
    medecin_ids.append(mid)

for _ in range(50):
    patho = random.choice(PATHOLOGIES)
    make_professionnel("INFIRMIER", SERVICES[patho])

# ── 3. Patients : 200, pathologie + baseline cohérentes avec risklevel ───────
patient_sql = []
patient_ids = []
RISK_WEIGHTS = [("NORMAL", 65), ("INFORMATIVE", 18), ("URGENTE", 12), ("CRITIQUE", 5)]
risk_pool = [r for r, w in RISK_WEIGHTS for _ in range(w)]

def baseline_for(patho, risk):
    # Valeurs de base plausibles, dégradées si risque élevé (cohérent avec les
    # seuils réels d'IoTProcessor/AlertProcessor : glucose CRITIQUE <0.60/>4.00 g/L,
    # SpO2 CRITIQUE <85%, poids URGENTE >84kg, tension URGENTE >160mmHg).
    glucose, spo2, systolic, weight = 1.0, 97.0, 120.0, 70.0
    if patho == "Diabète T2":
        glucose = {"NORMAL": random.uniform(0.9, 1.3), "INFORMATIVE": random.uniform(1.3, 1.8),
                   "URGENTE": random.uniform(1.8, 2.4), "CRITIQUE": random.uniform(2.5, 3.5)}[risk]
    if patho == "Insuffisance cardiaque":
        systolic = {"NORMAL": random.uniform(110, 135), "INFORMATIVE": random.uniform(135, 150),
                    "URGENTE": random.uniform(155, 170), "CRITIQUE": random.uniform(175, 195)}[risk]
        # weight non-tiéré à l'origine -> des patients NORMAL/INFORMATIVE
        # pouvaient hériter d'un poids déjà >84kg (seuil URGENTE d'AlertProcessor),
        # générant une alerte à quasi chaque mesure simulée. Tiéré comme les
        # autres vitaux pour rester cohérent avec le risklevel déclaré.
        weight = {"NORMAL": random.uniform(65, 78), "INFORMATIVE": random.uniform(78, 83),
                  "URGENTE": random.uniform(85, 92), "CRITIQUE": random.uniform(93, 105)}[risk]
    if patho == "BPCO":
        spo2 = {"NORMAL": random.uniform(95, 99), "INFORMATIVE": random.uniform(92, 95),
                "URGENTE": random.uniform(87, 91), "CRITIQUE": random.uniform(80, 86)}[risk]
    return round(glucose, 2), round(spo2, 1), round(systolic, 1), round(weight, 1)

for i in range(200):
    prenom, nom, sexe = rand_name()
    patho = random.choice(PATHOLOGIES)
    risk = random.choice(risk_pool)
    age = random.randint(28, 92)
    dob = (now - timedelta(days=age * 365 + random.randint(0, 364))).strftime("%Y-%m-%d")
    glucose, spo2, systolic, weight = baseline_for(patho, risk)
    pid = uid()
    patient_ids.append({
        "id": pid, "pathology": patho, "riskLevel": risk, "firstName": prenom, "lastName": nom,
        "baseGlucose": glucose, "baseSpO2": spo2, "baseSystolic": systolic, "weightKg": weight,
    })
    ins_nir = f"{random.randint(1,2)}{random.randint(50,99)}{random.randint(1,12):02d}{random.randint(1,95):02d}{random.randint(1,999):03d}{random.randint(1,999):03d}{random.randint(10,99)}"
    patient_sql.append(
        f"INSERT INTO patients (id, firstname, lastname, age, gender, dateofbirth, insnir, pathology, service, "
        f"assignedmedicid, risklevel, baseglucose, basespo2, basesystolic, weightkg, active, createdat, updatedat) VALUES "
        f"('{pid}', '{esc(prenom)}', '{esc(nom)}', {age}, '{ 'M' if sexe=='H' else 'F' }', '{dob}', '{ins_nir}', "
        f"'{esc(patho)}', '{esc(SERVICES[patho])}', '{random.choice(medecin_ids)}', '{risk}', "
        f"{glucose}, {spo2}, {systolic}, {weight}, true, '{iso(now)}', '{iso(now)}');"
    )

# ── 4. Dispositifs IoT : 10, assignés à des patients actifs au hasard ────────
DEVICE_SPECS = [
    ("GLUCOMETER",     "g/L", "2339-0",  "Abbott",  "FreeStyle Libre 3", 5),
    ("PULSE_OXIMETER", "%",   "59408-5", "Nonin",   "OnyxII 9560",       3),
    ("SCALE",          "kg",  "29463-7", "Withings", "Body Cardio",      2),
]
iot_sql = []
simulator_devices = []
n_serial = 1
for devtype, unit, loinc, manuf, model, count in DEVICE_SPECS:
    for _ in range(count):
        serial = f"DEV-{devtype[:3]}-{n_serial:03d}"
        n_serial += 1
        patient = random.choice(patient_ids)
        pid = patient["id"]
        iot_sql.append(
            f"INSERT INTO iot_devices (serial, patientid, devicetype, manufacturer, model, loinccode, unit, "
            f"active, currentscenario, registeredat, lastobservedat, lastvalue) VALUES "
            f"('{serial}', '{pid}', '{devtype}', '{manuf}', '{esc(model)}', '{loinc}', '{unit}', true, NULL, "
            f"'{iso(now)}', NULL, NULL);"
        )
        simulator_devices.append({
            "serial": serial, "patientId": pid, "patientName": f"{patient['firstName']} {patient['lastName']}",
            "deviceType": devtype, "unit": unit, "loincCode": loinc,
            "pathology": patient["pathology"], "riskLevel": patient["riskLevel"],
            "baseGlucose": patient["baseGlucose"], "baseSpO2": patient["baseSpO2"],
            "baseSystolic": patient["baseSystolic"], "weightKg": patient["weightKg"],
        })

# ── Écriture des fichiers ─────────────────────────────────────────────────────
import os
outdir = os.path.dirname(os.path.abspath(__file__))

with open(f"{outdir}/01_etablissement.sql", "w") as f:
    f.write("\n".join(etab_sql + prof_sql) + "\n")

with open(f"{outdir}/02_patient.sql", "w") as f:
    f.write("\n".join(patient_sql) + "\n")

with open(f"{outdir}/03_iot.sql", "w") as f:
    f.write("\n".join(iot_sql) + "\n")

# Fichier de référence pour le simulateur MQTT (mapping device -> patient/baseline)
import json
with open(f"{outdir}/devices_for_simulator.json", "w") as f:
    json.dump(simulator_devices, f, indent=2, ensure_ascii=False)

print(f"Établissements: {len(etab_sql)} | Professionnels: {len(prof_sql)} (20 médecins + 50 infirmiers)")
print(f"Patients: {len(patient_sql)}")
print(f"Dispositifs IoT: {len(iot_sql)}")
