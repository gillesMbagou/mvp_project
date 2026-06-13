package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.messaging.MessageReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageReadReceiptRepository extends JpaRepository<MessageReadReceipt, Long> {
    Optional<MessageReadReceipt> findByMessageIdAndReaderEmail(Long messageId, String readerEmail);
    List<MessageReadReceipt> findByMessageId(Long messageId);
}
