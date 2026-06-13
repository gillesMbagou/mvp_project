package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.messaging.SecureMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecureMessageRepository extends JpaRepository<SecureMessage, Long> {
    List<SecureMessage> findByThreadIdAndDeletedFalseOrderBySentAtAsc(Long threadId);
    long countByThreadIdAndDeletedFalse(Long threadId);
}
