package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.messaging.ThreadParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThreadParticipantRepository extends JpaRepository<ThreadParticipant, Long> {
    List<ThreadParticipant> findByThreadIdAndActiveTrue(Long threadId);
    List<ThreadParticipant> findByProfessionalEmailAndActiveTrue(String email);
}
