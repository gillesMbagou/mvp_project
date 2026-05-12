import { useState } from "react";

const DOMAINS = {
  acteurs:   { label: "Acteurs",               color: "#1A5C8A", light: "#D6E9F8", border: "#1A5C8A" },
  patient:   { label: "Patient",               color: "#0E7C7B", light: "#D4EEEE", border: "#0E7C7B" },
  soins:     { label: "Plans de Soins",         color: "#6B3FA0", light: "#EDE0F7", border: "#6B3FA0" },
  obs:       { label: "Observations & IoT",     color: "#D35400", light: "#FAE5D3", border: "#D35400" },
  pharma:    { label: "Prescriptions",          color: "#1E8449", light: "#D5F5E3", border: "#1E8449" },
  alertes:   { label: "Alertes",               color: "#C0392B", light: "#FADBD8", border: "#C0392B" },
  messaging: { label: "Messagerie",            color: "#7D6608", light: "#FEF9E7", border: "#B7950B" },
  audit:     { label: "Audit",                 color: "#515A5A", light: "#EAECEE", border: "#717D7E" },
};

const ENTITIES = [
  // ── Acteurs ────────────────────────────────────────────────────────────────
  {
    id: "ETABLISSEMENT", domain: "acteurs", label: "ETABLISSEMENT",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "nom", type: "VARCHAR(200)" },
      { name: "type", type: "ENUM(hôpital,clinique,cpts)" },
      { name: "finess_code", type: "CHAR(9)", unique: true },
      { name: "adresse", type: "TEXT" },
      { name: "schema_tenant", type: "VARCHAR(50)", unique: true },
      { name: "actif", type: "BOOLEAN" },
      { name: "created_at", type: "TIMESTAMPTZ" },
    ]
  },
  {
    id: "PROFESSIONNEL", domain: "acteurs", label: "PROFESSIONNEL",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "etablissement_id", type: "UUID", fk: "ETABLISSEMENT" },
      { name: "rpps_number", type: "CHAR(11)", unique: true },
      { name: "prenom", type: "VARCHAR(100)" },
      { name: "nom", type: "VARCHAR(100)" },
      { name: "role", type: "ENUM(médecin,infirmier,pharmacien,admin)" },
      { name: "specialite", type: "VARCHAR(100)" },
      { name: "email_mssante", type: "VARCHAR(200)" },
      { name: "actif", type: "BOOLEAN" },
    ]
  },

  // ── Patient ────────────────────────────────────────────────────────────────
  {
    id: "PATIENT", domain: "patient", label: "PATIENT",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "medecin_traitant_id", type: "UUID", fk: "PROFESSIONNEL" },
      { name: "ins_nir", type: "VARCHAR(15) ENCRYPTED", unique: true },
      { name: "prenom", type: "VARCHAR(100)" },
      { name: "nom", type: "VARCHAR(100)" },
      { name: "date_naissance", type: "DATE" },
      { name: "genre", type: "ENUM(M,F,I)" },
      { name: "adresse", type: "TEXT ENCRYPTED" },
      { name: "created_at", type: "TIMESTAMPTZ" },
    ]
  },
  {
    id: "PATIENT_CONDITION", domain: "patient", label: "PATIENT_CONDITION",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "patient_id", type: "UUID", fk: "PATIENT" },
      { name: "code_cim10", type: "VARCHAR(10)" },
      { name: "libelle", type: "VARCHAR(300)" },
      { name: "date_debut", type: "DATE" },
      { name: "date_fin", type: "DATE" },
      { name: "actif", type: "BOOLEAN" },
    ]
  },
  {
    id: "EQUIPE_SOINS", domain: "patient", label: "EQUIPE_SOINS",
    attrs: [
      { name: "patient_id", type: "UUID", pk: true, fk: "PATIENT" },
      { name: "professionnel_id", type: "UUID", pk: true, fk: "PROFESSIONNEL" },
      { name: "role_dans_equipe", type: "VARCHAR(100)" },
      { name: "date_entree", type: "DATE" },
      { name: "date_sortie", type: "DATE" },
    ]
  },

  // ── Plans de Soins ─────────────────────────────────────────────────────────
  {
    id: "CARE_PLAN", domain: "soins", label: "CARE_PLAN",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "patient_id", type: "UUID", fk: "PATIENT" },
      { name: "created_by", type: "UUID", fk: "PROFESSIONNEL" },
      { name: "condition_id", type: "UUID", fk: "PATIENT_CONDITION" },
      { name: "titre", type: "VARCHAR(300)" },
      { name: "statut", type: "ENUM(actif,terminé,suspendu)" },
      { name: "date_debut", type: "DATE" },
      { name: "date_fin", type: "DATE" },
      { name: "protocole_ref", type: "VARCHAR(100)" },
    ]
  },
  {
    id: "CARE_PLAN_TASK", domain: "soins", label: "CARE_PLAN_TASK",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "plan_id", type: "UUID", fk: "CARE_PLAN" },
      { name: "assignee_id", type: "UUID", fk: "PROFESSIONNEL" },
      { name: "type", type: "ENUM(consultation,bilan,acte,éducation)" },
      { name: "description", type: "TEXT" },
      { name: "echeance", type: "TIMESTAMPTZ" },
      { name: "statut", type: "ENUM(à_faire,en_cours,terminée,annulée)" },
      { name: "completee_le", type: "TIMESTAMPTZ" },
    ]
  },

  // ── Observations & IoT ─────────────────────────────────────────────────────
  {
    id: "OBSERVATION", domain: "obs", label: "OBSERVATION",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "patient_id", type: "UUID", fk: "PATIENT" },
      { name: "professionnel_id", type: "UUID", fk: "PROFESSIONNEL" },
      { name: "device_id", type: "UUID", fk: "IOT_DEVICE" },
      { name: "code_loinc", type: "VARCHAR(20)" },
      { name: "valeur_numerique", type: "NUMERIC(12,4)" },
      { name: "valeur_texte", type: "TEXT" },
      { name: "unite", type: "VARCHAR(50)" },
      { name: "date_obs", type: "TIMESTAMPTZ" },
      { name: "source", type: "ENUM(manuel,iot,labo,imagerie)" },
    ]
  },
  {
    id: "IOT_DEVICE", domain: "obs", label: "IOT_DEVICE",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "patient_id", type: "UUID", fk: "PATIENT" },
      { name: "type", type: "ENUM(glucomètre,tensiomètre,oxymètre,balance,ecg,patch)" },
      { name: "marque", type: "VARCHAR(100)" },
      { name: "modele", type: "VARCHAR(100)" },
      { name: "serial_number", type: "VARCHAR(100)", unique: true },
      { name: "protocole", type: "ENUM(BLE,MQTT,WiFi,LPWAN)" },
      { name: "actif", type: "BOOLEAN" },
      { name: "date_activation", type: "DATE" },
    ]
  },

  // ── Prescriptions ──────────────────────────────────────────────────────────
  {
    id: "MEDICAMENT", domain: "pharma", label: "MEDICAMENT",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "cis_code", type: "VARCHAR(20)", unique: true },
      { name: "denomination", type: "VARCHAR(300)" },
      { name: "forme", type: "VARCHAR(100)" },
      { name: "voie", type: "VARCHAR(100)" },
      { name: "dci", type: "VARCHAR(200)" },
      { name: "classe_atc", type: "VARCHAR(10)" },
      { name: "actif", type: "BOOLEAN" },
    ]
  },
  {
    id: "PRESCRIPTION", domain: "pharma", label: "PRESCRIPTION",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "patient_id", type: "UUID", fk: "PATIENT" },
      { name: "medecin_id", type: "UUID", fk: "PROFESSIONNEL" },
      { name: "date_prescription", type: "TIMESTAMPTZ" },
      { name: "statut", type: "ENUM(brouillon,signée,délivrée,expirée)" },
      { name: "signature_eidas", type: "TEXT" },
      { name: "valide_jusqu_au", type: "DATE" },
    ]
  },
  {
    id: "PRESCRIPTION_LINE", domain: "pharma", label: "PRESCRIPTION_LINE",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "prescription_id", type: "UUID", fk: "PRESCRIPTION" },
      { name: "medicament_id", type: "UUID", fk: "MEDICAMENT" },
      { name: "posologie", type: "TEXT" },
      { name: "duree_jours", type: "SMALLINT" },
      { name: "renouvellements", type: "SMALLINT" },
      { name: "substitution_ok", type: "BOOLEAN" },
    ]
  },

  // ── Alertes ────────────────────────────────────────────────────────────────
  {
    id: "ALERTE", domain: "alertes", label: "ALERTE",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "patient_id", type: "UUID", fk: "PATIENT" },
      { name: "observation_id", type: "UUID", fk: "OBSERVATION" },
      { name: "acquitee_par", type: "UUID", fk: "PROFESSIONNEL" },
      { name: "type", type: "ENUM(biologique,médicamenteuse,observance,iot,prédictive)" },
      { name: "severite", type: "ENUM(critique,urgente,informative)" },
      { name: "message", type: "TEXT" },
      { name: "acquitee_le", type: "TIMESTAMPTZ" },
      { name: "created_at", type: "TIMESTAMPTZ" },
    ]
  },

  // ── Messagerie ─────────────────────────────────────────────────────────────
  {
    id: "MESSAGE_SECURISE", domain: "messaging", label: "MESSAGE_SECURISE",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "expediteur_id", type: "UUID", fk: "PROFESSIONNEL" },
      { name: "patient_id", type: "UUID", fk: "PATIENT" },
      { name: "sujet", type: "VARCHAR(300)" },
      { name: "contenu_chiffre", type: "TEXT ENCRYPTED" },
      { name: "date_envoi", type: "TIMESTAMPTZ" },
      { name: "priorite", type: "ENUM(normale,urgente)" },
    ]
  },
  {
    id: "MESSAGE_DESTINATAIRE", domain: "messaging", label: "MESSAGE_DESTINATAIRE",
    attrs: [
      { name: "message_id", type: "UUID", pk: true, fk: "MESSAGE_SECURISE" },
      { name: "destinataire_id", type: "UUID", pk: true, fk: "PROFESSIONNEL" },
      { name: "lu_le", type: "TIMESTAMPTZ" },
    ]
  },

  // ── Audit ──────────────────────────────────────────────────────────────────
  {
    id: "AUDIT_LOG", domain: "audit", label: "AUDIT_LOG",
    attrs: [
      { name: "id", type: "UUID", pk: true },
      { name: "user_id", type: "UUID", fk: "PROFESSIONNEL" },
      { name: "patient_id", type: "UUID", fk: "PATIENT" },
      { name: "action", type: "VARCHAR(100)" },
      { name: "resource_type", type: "VARCHAR(100)" },
      { name: "resource_id", type: "UUID" },
      { name: "ip_address", type: "INET" },
      { name: "timestamp", type: "TIMESTAMPTZ" },
    ]
  },
];

const RELATIONS = [
  // ETABLISSEMENT
  { from: "PROFESSIONNEL", to: "ETABLISSEMENT", label: "appartient à", card: "N..1" },
  // PATIENT
  { from: "PATIENT", to: "PROFESSIONNEL", label: "médecin traitant", card: "N..1" },
  { from: "PATIENT_CONDITION", to: "PATIENT", label: "concerne", card: "N..1" },
  { from: "EQUIPE_SOINS", to: "PATIENT", label: "patient", card: "N..1" },
  { from: "EQUIPE_SOINS", to: "PROFESSIONNEL", label: "professionnel", card: "N..1" },
  // CARE PLAN
  { from: "CARE_PLAN", to: "PATIENT", label: "pour", card: "N..1" },
  { from: "CARE_PLAN", to: "PROFESSIONNEL", label: "créé par", card: "N..1" },
  { from: "CARE_PLAN", to: "PATIENT_CONDITION", label: "lié à", card: "N..1" },
  { from: "CARE_PLAN_TASK", to: "CARE_PLAN", label: "appartient à", card: "N..1" },
  { from: "CARE_PLAN_TASK", to: "PROFESSIONNEL", label: "assignée à", card: "N..1" },
  // OBSERVATION & IoT
  { from: "OBSERVATION", to: "PATIENT", label: "mesure de", card: "N..1" },
  { from: "OBSERVATION", to: "PROFESSIONNEL", label: "saisie par", card: "N..0..1" },
  { from: "OBSERVATION", to: "IOT_DEVICE", label: "issue de", card: "N..0..1" },
  { from: "IOT_DEVICE", to: "PATIENT", label: "attribué à", card: "N..1" },
  // PRESCRIPTION
  { from: "PRESCRIPTION", to: "PATIENT", label: "prescrite à", card: "N..1" },
  { from: "PRESCRIPTION", to: "PROFESSIONNEL", label: "prescrite par", card: "N..1" },
  { from: "PRESCRIPTION_LINE", to: "PRESCRIPTION", label: "ligne de", card: "N..1" },
  { from: "PRESCRIPTION_LINE", to: "MEDICAMENT", label: "contient", card: "N..1" },
  // ALERTE
  { from: "ALERTE", to: "PATIENT", label: "concerne", card: "N..1" },
  { from: "ALERTE", to: "OBSERVATION", label: "déclenchée par", card: "N..0..1" },
  { from: "ALERTE", to: "PROFESSIONNEL", label: "acquittée par", card: "N..0..1" },
  // MESSAGERIE
  { from: "MESSAGE_SECURISE", to: "PROFESSIONNEL", label: "expédié par", card: "N..1" },
  { from: "MESSAGE_SECURISE", to: "PATIENT", label: "au sujet de", card: "N..0..1" },
  { from: "MESSAGE_DESTINATAIRE", to: "MESSAGE_SECURISE", label: "reçu par", card: "N..1" },
  { from: "MESSAGE_DESTINATAIRE", to: "PROFESSIONNEL", label: "destinataire", card: "N..1" },
  // AUDIT
  { from: "AUDIT_LOG", to: "PROFESSIONNEL", label: "action de", card: "N..1" },
  { from: "AUDIT_LOG", to: "PATIENT", label: "concerne", card: "N..0..1" },
];

function EntityCard({ entity, isSelected, onClick }) {
  const domain = DOMAINS[entity.domain];
  return (
    <div
      onClick={() => onClick(entity.id)}
      style={{
        cursor: "pointer",
        borderRadius: 8,
        border: `2px solid ${isSelected ? domain.color : domain.border + "99"}`,
        boxShadow: isSelected
          ? `0 0 0 3px ${domain.color}44, 0 4px 16px ${domain.color}33`
          : "0 2px 8px rgba(0,0,0,0.10)",
        background: "#fff",
        minWidth: 240,
        transition: "box-shadow 0.2s, border-color 0.2s",
        fontFamily: "'JetBrains Mono', 'Fira Code', 'Courier New', monospace",
        fontSize: 12,
        overflow: "hidden",
      }}
    >
      {/* Header */}
      <div style={{
        background: domain.color,
        padding: "8px 12px",
        display: "flex",
        alignItems: "center",
        gap: 8,
      }}>
        <span style={{ color: "#fff", fontWeight: 700, fontSize: 12, letterSpacing: 1 }}>
          {entity.label}
        </span>
        <span style={{
          background: domain.light,
          color: domain.color,
          fontSize: 10,
          fontWeight: 700,
          padding: "1px 7px",
          borderRadius: 20,
          marginLeft: "auto",
          letterSpacing: 0.5,
        }}>
          {domain.label}
        </span>
      </div>
      {/* Attributes */}
      <div style={{ padding: "6px 0" }}>
        {entity.attrs.map((a) => (
          <div key={a.name} style={{
            display: "flex",
            alignItems: "center",
            gap: 6,
            padding: "3px 12px",
            background: i % 2 === 0 ? "#FAFAFA" : "#fff",
            borderLeft: a.pk ? `3px solid ${domain.color}` : a.fk ? `3px solid ${DOMAINS[ENTITIES.find(e => e.id === a.fk)?.domain]?.color || "#aaa"}` : "3px solid transparent",
          }}>
            {a.pk && (
              <span style={{ color: domain.color, fontWeight: 900, fontSize: 11 }} title="Primary Key">🔑</span>
            )}
            {a.fk && !a.pk && (
              <span style={{ color: "#888", fontSize: 10 }} title={`FK → ${a.fk}`}>🔗</span>
            )}
            {!a.pk && !a.fk && <span style={{ width: 16 }} />}
            <span style={{
              color: a.pk ? domain.color : a.fk ? "#555" : "#222",
              fontWeight: a.pk ? 700 : a.fk ? 600 : 400,
              flex: 1,
              fontSize: 11.5,
            }}>
              {a.name}
            </span>
            <span style={{
              color: "#999",
              fontSize: 10,
              fontStyle: "italic",
              textAlign: "right",
              maxWidth: 130,
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            }}>
              {a.type}
            </span>
            {a.unique && (
              <span style={{ color: "#E67E22", fontSize: 9, fontWeight: 700, marginLeft: 2 }}>UQ</span>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

function RelationRow({ rel }) {
  const fromEntity = ENTITIES.find(e => e.id === rel.from);
  const toEntity = ENTITIES.find(e => e.id === rel.to);
  const domFrom = DOMAINS[fromEntity?.domain];
  const domTo = DOMAINS[toEntity?.domain];
  return (
    <div style={{
      display: "flex",
      alignItems: "center",
      gap: 8,
      padding: "5px 12px",
      borderBottom: "1px solid #f0f0f0",
      fontSize: 12,
      fontFamily: "'JetBrains Mono', monospace",
    }}>
      <span style={{
        background: domFrom?.light,
        color: domFrom?.color,
        fontWeight: 700,
        padding: "2px 8px",
        borderRadius: 4,
        fontSize: 11,
        minWidth: 180,
        textAlign: "center",
      }}>
        {rel.from}
      </span>
      <span style={{ color: "#888", fontSize: 11, minWidth: 70, textAlign: "center" }}>
        ──{rel.card}──▶
      </span>
      <span style={{
        background: domTo?.light,
        color: domTo?.color,
        fontWeight: 700,
        padding: "2px 8px",
        borderRadius: 4,
        fontSize: 11,
        minWidth: 180,
        textAlign: "center",
      }}>
        {rel.to}
      </span>
      <span style={{ color: "#666", fontStyle: "italic", fontSize: 11, marginLeft: 8 }}>
        « {rel.label} »
      </span>
    </div>
  );
}

export default function CareSync_ERD() {
  const [selectedEntity, setSelectedEntity] = useState(null);
  const [activeTab, setActiveTab] = useState("entities");
  const [filterDomain, setFilterDomain] = useState("all");

  const handleEntityClick = (id) => {
    setSelectedEntity(prev => prev === id ? null : id);
  };

  const relatedRelations = selectedEntity
    ? RELATIONS.filter(r => r.from === selectedEntity || r.to === selectedEntity)
    : [];

  const filteredEntities = filterDomain === "all"
    ? ENTITIES
    : ENTITIES.filter(e => e.domain === filterDomain);

  const groupedEntities = {};
  filteredEntities.forEach(e => {
    if (!groupedEntities[e.domain]) groupedEntities[e.domain] = [];
    groupedEntities[e.domain].push(e);
  });

  return (
    <div style={{
      fontFamily: "'Segoe UI', system-ui, sans-serif",
      background: "#F4F6F9",
      minHeight: "100vh",
      color: "#222",
    }}>
      {/* Header */}
      <div style={{
        background: "linear-gradient(135deg, #1A5C8A 0%, #0E7C7B 100%)",
        padding: "20px 32px",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        boxShadow: "0 2px 12px rgba(0,0,0,0.18)",
      }}>
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <span style={{ fontSize: 28 }}>🏥</span>
            <span style={{ color: "#fff", fontWeight: 800, fontSize: 22, letterSpacing: 1 }}>
              CareSync
            </span>
            <span style={{ color: "#BDD7EE", fontWeight: 400, fontSize: 14, marginLeft: 4 }}>
              — Modèle Entité-Association
            </span>
          </div>
          <div style={{ color: "#BDD7EE", fontSize: 12, marginTop: 4 }}>
            {ENTITIES.length} entités · {RELATIONS.length} relations · PostgreSQL 16 multi-tenant
          </div>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          {["entities", "relations"].map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              style={{
                background: activeTab === tab ? "#fff" : "rgba(255,255,255,0.15)",
                color: activeTab === tab ? "#1A5C8A" : "#fff",
                border: "none",
                borderRadius: 6,
                padding: "8px 18px",
                fontWeight: 600,
                fontSize: 13,
                cursor: "pointer",
                transition: "all 0.2s",
              }}
            >
              {tab === "entities" ? "📐 Entités" : "🔗 Relations"}
            </button>
          ))}
        </div>
      </div>

      {/* Domain Filter */}
      <div style={{
        background: "#fff",
        borderBottom: "1px solid #e0e0e0",
        padding: "10px 32px",
        display: "flex",
        alignItems: "center",
        gap: 8,
        flexWrap: "wrap",
      }}>
        <span style={{ fontSize: 12, color: "#666", fontWeight: 600, marginRight: 4 }}>
          Domaine :
        </span>
        <button
          onClick={() => setFilterDomain("all")}
          style={{
            background: filterDomain === "all" ? "#1A5C8A" : "#F0F0F0",
            color: filterDomain === "all" ? "#fff" : "#444",
            border: "none", borderRadius: 20,
            padding: "4px 14px", fontSize: 12, fontWeight: 600, cursor: "pointer",
          }}
        >
          Tous
        </button>
        {Object.entries(DOMAINS).map(([key, d]) => (
          <button
            key={key}
            onClick={() => setFilterDomain(key)}
            style={{
              background: filterDomain === key ? d.color : d.light,
              color: filterDomain === key ? "#fff" : d.color,
              border: `1px solid ${d.border}`,
              borderRadius: 20,
              padding: "4px 14px", fontSize: 12, fontWeight: 600, cursor: "pointer",
            }}
          >
            {d.label}
          </button>
        ))}
      </div>

      {/* Selected entity info */}
      {selectedEntity && relatedRelations.length > 0 && (
        <div style={{
          background: "#FFF8E1",
          borderBottom: "2px solid #F9A825",
          padding: "10px 32px",
          display: "flex",
          alignItems: "center",
          gap: 16,
          flexWrap: "wrap",
        }}>
          <span style={{ fontWeight: 700, fontSize: 12, color: "#795548" }}>
            🔎 Relations de {selectedEntity} :
          </span>
          {relatedRelations.map((r) => (
            <span key={`${r.from}-${r.to}-${r.label}`} style={{
              background: "#fff",
              border: "1px solid #F9A825",
              borderRadius: 4,
              padding: "3px 10px",
              fontSize: 11,
              color: "#555",
              fontFamily: "monospace",
            }}>
              {r.from === selectedEntity
                ? `→ ${r.to} (${r.label})`
                : `← ${r.from} (${r.label})`}
            </span>
          ))}
          <button
            onClick={() => setSelectedEntity(null)}
            style={{
              background: "none", border: "none", color: "#888",
              cursor: "pointer", fontSize: 13, marginLeft: "auto",
            }}
          >
            ✕ Fermer
          </button>
        </div>
      )}

      {/* Main content */}
      <div style={{ padding: "24px 32px" }}>

        {activeTab === "entities" && (
          <>
            {/* Legend */}
            <div style={{
              background: "#fff",
              borderRadius: 8,
              padding: "12px 20px",
              marginBottom: 20,
              display: "flex",
              alignItems: "center",
              gap: 24,
              flexWrap: "wrap",
              boxShadow: "0 1px 4px rgba(0,0,0,0.08)",
            }}>
              <span style={{ fontWeight: 700, fontSize: 12, color: "#444" }}>Légende :</span>
              <span style={{ fontSize: 12, color: "#444" }}>🔑 Clé primaire (PK)</span>
              <span style={{ fontSize: 12, color: "#444" }}>🔗 Clé étrangère (FK)</span>
              <span style={{ fontSize: 12, color: "#E67E22" }}>UQ Contrainte unique</span>
              <span style={{ fontSize: 12, color: "#888", fontFamily: "monospace" }}>ENCRYPTED = chiffrement Vault Transit</span>
              <span style={{ fontSize: 12, color: "#888" }}>Cliquer sur une entité pour voir ses relations</span>
            </div>

            {/* Entities by domain */}
            {Object.entries(groupedEntities).map(([domKey, entities]) => {
              const domain = DOMAINS[domKey];
              return (
                <div key={domKey} style={{ marginBottom: 28 }}>
                  <div style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 10,
                    marginBottom: 12,
                    paddingBottom: 8,
                    borderBottom: `2px solid ${domain.color}`,
                  }}>
                    <div style={{
                      width: 14, height: 14, borderRadius: 3,
                      background: domain.color,
                    }} />
                    <span style={{
                      fontWeight: 800, fontSize: 14, color: domain.color,
                      textTransform: "uppercase", letterSpacing: 1,
                    }}>
                      {domain.label}
                    </span>
                    <span style={{
                      background: domain.light,
                      color: domain.color,
                      fontSize: 11, fontWeight: 600,
                      padding: "2px 10px", borderRadius: 20,
                    }}>
                      {entities.length} entité{entities.length > 1 ? "s" : ""}
                    </span>
                  </div>
                  <div style={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: 16,
                  }}>
                    {entities.map(e => (
                      <EntityCard
                        key={e.id}
                        entity={e}
                        isSelected={selectedEntity === e.id}
                        onClick={handleEntityClick}
                      />
                    ))}
                  </div>
                </div>
              );
            })}
          </>
        )}

        {activeTab === "relations" && (
          <div style={{
            background: "#fff",
            borderRadius: 8,
            boxShadow: "0 1px 4px rgba(0,0,0,0.08)",
            overflow: "hidden",
          }}>
            {/* Header */}
            <div style={{
              display: "flex",
              alignItems: "center",
              gap: 8,
              padding: "12px 16px",
              background: "#1A5C8A",
              color: "#fff",
              fontWeight: 700,
              fontSize: 12,
              fontFamily: "monospace",
            }}>
              <span style={{ minWidth: 180, textAlign: "center" }}>ENTITÉ SOURCE</span>
              <span style={{ minWidth: 70, textAlign: "center" }}>CARDINALITÉ</span>
              <span style={{ minWidth: 180, textAlign: "center" }}>ENTITÉ CIBLE</span>
              <span style={{ marginLeft: 8 }}>SÉMANTIQUE</span>
            </div>
            {RELATIONS.map((r) => (
              <RelationRow key={`${r.from}-${r.to}-${r.label}`} rel={r} />
            ))}
            {/* Summary */}
            <div style={{
              padding: "16px 24px",
              background: "#F4F6F9",
              borderTop: "2px solid #e0e0e0",
            }}>
              <div style={{ fontWeight: 700, fontSize: 13, color: "#1A5C8A", marginBottom: 8 }}>
                Cardinalités utilisées
              </div>
              <div style={{ display: "flex", gap: 24, flexWrap: "wrap" }}>
                {[
                  { card: "N..1", desc: "Plusieurs → Un (obligatoire)" },
                  { card: "N..0..1", desc: "Plusieurs → Zéro ou un (optionnel)" },
                  { card: "1..1", desc: "Un → Un (obligatoire)" },
                ].map(c => (
                  <div key={c.card} style={{
                    display: "flex", alignItems: "center", gap: 8, fontSize: 12,
                  }}>
                    <code style={{
                      background: "#fff",
                      border: "1px solid #1A5C8A",
                      color: "#1A5C8A",
                      padding: "2px 8px",
                      borderRadius: 4,
                      fontWeight: 700,
                    }}>
                      {c.card}
                    </code>
                    <span style={{ color: "#555" }}>{c.desc}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Stats bar */}
        <div style={{
          marginTop: 24,
          display: "flex",
          gap: 12,
          flexWrap: "wrap",
        }}>
          {[
            { label: "Tables", value: ENTITIES.length, color: "#1A5C8A" },
            { label: "Relations", value: RELATIONS.length, color: "#0E7C7B" },
            { label: "Attributs total", value: ENTITIES.reduce((s, e) => s + e.attrs.length, 0), color: "#6B3FA0" },
            { label: "Clés primaires", value: ENTITIES.reduce((s, e) => s + e.attrs.filter(a => a.pk).length, 0), color: "#D35400" },
            { label: "Clés étrangères", value: ENTITIES.reduce((s, e) => s + e.attrs.filter(a => a.fk).length, 0), color: "#1E8449" },
            { label: "Champs chiffrés", value: ENTITIES.reduce((s, e) => s + e.attrs.filter(a => a.type?.includes("ENCRYPTED")).length, 0), color: "#C0392B" },
          ].map(stat => (
            <div key={stat.label} style={{
              background: "#fff",
              borderRadius: 8,
              padding: "12px 20px",
              boxShadow: "0 1px 4px rgba(0,0,0,0.08)",
              borderLeft: `4px solid ${stat.color}`,
              minWidth: 130,
            }}>
              <div style={{ fontSize: 22, fontWeight: 800, color: stat.color }}>{stat.value}</div>
              <div style={{ fontSize: 11, color: "#888", marginTop: 2 }}>{stat.label}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
