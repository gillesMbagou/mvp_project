package be.caresync.demo.service;

import be.caresync.demo.model.db.prescription.DrugInteraction;
import be.caresync.demo.model.db.prescription.PrescriptionLine;
import be.caresync.demo.repository.DrugInteractionRepository;
import be.caresync.demo.repository.PrescriptionLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DrugInteractionService {

    private final DrugInteractionRepository interactionRepo;
    private final PrescriptionLineRepository lineRepo;

    @Transactional
    public void checkAndFlagInteractions(List<PrescriptionLine> lines) {
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
