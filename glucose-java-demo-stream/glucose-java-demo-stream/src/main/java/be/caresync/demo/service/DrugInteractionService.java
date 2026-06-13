package be.caresync.demo.service;

import be.caresync.demo.model.db.prescription.DrugInteraction;
import be.caresync.demo.model.db.prescription.PrescriptionLine;
import be.caresync.demo.model.db.patient.Medication;
import be.caresync.demo.repository.jpa.DrugInteractionRepository;
import be.caresync.demo.repository.jpa.PrescriptionLineRepository;
import be.caresync.demo.repository.jpa.MedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DrugInteractionService {

    private final DrugInteractionRepository interactionRepo;
    private final PrescriptionLineRepository lineRepo;
    private final MedicationRepository medicationRepo;

    @Transactional
    public void checkAndFlagInteractions(String patientId, List<PrescriptionLine> lines) {
        // 1. Vérification des interactions entre les nouvelles lignes elles-mêmes
        for (int i = 0; i < lines.size(); i++) {
            for (int j = i + 1; j < lines.size(); j++) {
                PrescriptionLine lineA = lines.get(i);
                PrescriptionLine lineB = lines.get(j);
                if (lineA.getAtcCode() == null || lineB.getAtcCode() == null) continue;

                List<DrugInteraction> interactions = interactionRepo.findInteractionBetween(
                        lineA.getAtcCode(), lineB.getAtcCode());

                if (!interactions.isEmpty()) {
                    DrugInteraction worst = interactions.stream()
                            .max((a, b) -> a.getSeverity().compareTo(b.getSeverity()))
                            .get();
                    String detail = worst.getSeverity() + ": " + worst.getClinicalConsequence();
                    lineA.setHasInteractionWarning(true);
                    lineA.setInteractionDetails(detail);
                    lineB.setHasInteractionWarning(true);
                    lineB.setInteractionDetails(detail);
                    lineRepo.save(lineA);
                    lineRepo.save(lineB);
                }
            }
        }

        // 2. Vérification des interactions avec les traitements en cours du patient
        if (patientId != null) {
            List<Medication> ongoingMeds = medicationRepo.findByPatientIdAndOngoingTrue(patientId);
            for (PrescriptionLine newLine : lines) {
                if (newLine.getAtcCode() == null) continue;
                for (Medication activeMed : ongoingMeds) {
                    if (activeMed.getAtcCode() == null) continue;

                    List<DrugInteraction> interactions = interactionRepo.findInteractionBetween(
                            newLine.getAtcCode(), activeMed.getAtcCode());

                    if (!interactions.isEmpty()) {
                        DrugInteraction worst = interactions.stream()
                                .max((a, b) -> a.getSeverity().compareTo(b.getSeverity()))
                                .get();
                        String detail = worst.getSeverity() + " (avec traitement en cours " + activeMed.getName() + ") : " + worst.getClinicalConsequence();
                        newLine.setHasInteractionWarning(true);
                        String existingDetails = newLine.getInteractionDetails();
                        newLine.setInteractionDetails(existingDetails != null ? existingDetails + " | " + detail : detail);
                        lineRepo.save(newLine);
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<DrugInteraction> getInteractionsForAtc(String atcCode) {
        return interactionRepo.findByAtcCodeAOrAtcCodeB(atcCode, atcCode);
    }

    @Transactional
    public DrugInteraction addInteraction(DrugInteraction interaction) {
        return interactionRepo.save(interaction);
    }
}
