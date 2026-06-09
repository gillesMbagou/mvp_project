package be.caresync.demo.repository;

import be.caresync.demo.model.db.prescription.DrugInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, Long> {

    @Query("SELECT d FROM DrugInteraction d WHERE (d.atcCodeA = :atcA AND d.atcCodeB = :atcB) OR (d.atcCodeA = :atcB AND d.atcCodeB = :atcA)")
    List<DrugInteraction> findInteractionBetween(@Param("atcA") String atcA, @Param("atcB") String atcB);

    List<DrugInteraction> findByAtcCodeAOrAtcCodeB(String atcCodeA, String atcCodeB);
}
