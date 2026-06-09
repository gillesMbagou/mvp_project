package be.caresync.demo.repository;

import be.caresync.demo.model.db.messaging.MessageThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageThreadRepository extends JpaRepository<MessageThread, Long> {
    List<MessageThread> findByRelatedPatientIdAndActiveTrue(String patientId);
    List<MessageThread> findByActiveTrue();
}
