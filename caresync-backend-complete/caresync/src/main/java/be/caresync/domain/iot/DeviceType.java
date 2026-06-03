package be.caresync.domain.iot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeviceType {

    GLUCOMETER      ("Glucomètre",       "2345-7",   "g/L",   0.2,  6.0),
    PULSE_OXIMETER  ("Oxymètre",         "2708-6",   "%",     50.0, 100.0),
    BLOOD_PRESSURE  ("Tensiomètre",      "55284-4",  "mmHg",  40.0, 300.0),
    SCALE           ("Balance",          "29463-7",  "kg",    10.0, 300.0),
    HEART_RATE      ("Moniteur FC",      "8867-4",   "bpm",   20.0, 300.0),
    TEMPERATURE     ("Thermomètre",      "8310-5",   "°C",    30.0, 45.0),
    ECG             ("ECG portable",     "11524-6",  "mV",    -5.0, 5.0),
    SPO2            ("Capteur SpO2",     "59408-5",  "%",     50.0, 100.0);

    private final String label;
    private final String loincCode;        // Code LOINC(Logical Observation Identifiers Names and Codes) :
                                           // C'est un système de codification universel pour nommer les observations
                                           // médicales et biologiques. Créé en 1994 par le Regenstrief Institute (USA),
                                           // maintenu par une organisation internationale.
                                           //Le problème qu'il résout 👨‍⚕️ : chaque laboratoire, chaque fabricant de capteur,
                                           //chaque logiciel a ses propres noms pour les mêmes mesures. Sans standard commun, 
                                           //un glucomètre Accu-Check appelle la mesure glucose_cap, un autre l'appelle BG_reading,
                                           //un troisième blood_sugar. Impossible de croiser les données
                                           
                                           // La solution LOINC : chaque type de mesure a un code universel unique.
                                           //**************************************************************
                                           /* 
                                              2345-7   → Glucose [Masse/volume] dans le sang capillaire
                                              2708-6   → Oxygène [Saturation] dans le sang artériel (SpO2)
                                              8867-4   → Fréquence cardiaque
                                              29463-7  → Poids corporel
                                              55284-4  → Pression artérielle (systolique + diastolique)
                                              8310-5   → Température corporelle
                                              11524-6  → ECG (électrocardiogramme)
                                              */
                                              /**
                                                Un code LOINC est structuré en 5 axes : quoi mesure-t-on /
                                                avec quelle propriété / dans quel délai / dans quel système /
                                                avec quelle méthode. Le code 2345-7 signifie littéralement "glucose,
                                                masse par volume, mesure ponctuelle, sang capillaire, méthode non précisée                                              
                                              **/
                                           
                                          // FHIR(Fast Healthcare Interoperability Resources) : C'est un standard d'échange de données de santé
                                         // publié par HL7 International.  
                                        //La version courante est FHIR R4 (2019), et R5 est en cours d'adoption.
                                        /**
                                        Le problème qu'il résout : les systèmes de santé ne se parlent pas. 
                                        Un dossier patient dans un logiciel hospitalier est incompatible avec 
                                        le logiciel du médecin de ville, lui-même incompatible avec l'application 
                                        mobile du patient.La solution FHIR : définir des Resources standardisées —
                                        des objets JSON/XML représentant chaque concept médical — échangeables via
                                        une API REST.
                                        */
                                        /**
                                         Patient          → Identité du patient
                                          Observation      → Mesure clinique (glycémie, SpO2, poids...)
                                          Device           → Dispositif médical (glucomètre, oxymètre...)
                                          Alert / Flag     → Alerte clinique
                                          MedicationRequest → Ordonnance
                                          Condition        → Pathologie diagnostiquée
                                          Practitioner     → Professionnel de santé
                                          Organization     → Établissement de santé
                                        **/
    private final String unit;
    private final double physioMin;       // Plage physiologique acceptable
    private final double physioMax;

    public boolean isPhysiologicallyValid(double value) {
        return value >= physioMin && value <= physioMax;
    }
}
