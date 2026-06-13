package be.caresync.demo.service;

import be.caresync.demo.model.db.messaging.*;
import be.caresync.demo.repository.jpa.MessageReadReceiptRepository;
import be.caresync.demo.repository.jpa.MessageThreadRepository;
import be.caresync.demo.repository.jpa.SecureMessageRepository;
import be.caresync.demo.repository.jpa.ThreadParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecureMessagingService {

    private final MessageThreadRepository threadRepo;
    private final ThreadParticipantRepository participantRepo;
    private final SecureMessageRepository messageRepo;
    private final MessageReadReceiptRepository receiptRepo;
    private final AuditService auditService;

    @Transactional
    public MessageThread createThread(MessageThread thread, List<ThreadParticipant> participants) {
        thread.setCreatedAt(Instant.now());
        thread.setUpdatedAt(Instant.now());
        MessageThread saved = threadRepo.save(thread);
        participants.forEach(p -> {
            p.setThreadId(saved.getId());
            p.setJoinedAt(Instant.now());
            participantRepo.save(p);
        });
        auditService.log("CREATE_MESSAGE_THREAD", "MessageThread", String.valueOf(saved.getId()),
                saved.getRelatedPatientId(), saved.getCreatedBy(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MessageThread> getThreadsForPatient(String patientId) {
        return threadRepo.findByRelatedPatientIdAndActiveTrue(patientId);
    }

    @Transactional(readOnly = true)
    public Optional<MessageThread> getThreadById(Long id) {
        return threadRepo.findById(id);
    }

    @Transactional
    public SecureMessage sendMessage(SecureMessage message) {
        message.setSentAt(Instant.now());
        message.setStatus(SecureMessage.MessageStatus.SENT);
        SecureMessage saved = messageRepo.save(message);
        threadRepo.findById(message.getThreadId()).ifPresent(t -> {
            t.setUpdatedAt(Instant.now());
            threadRepo.save(t);
        });
        auditService.log("SEND_MESSAGE", "SecureMessage", String.valueOf(saved.getId()), null, saved.getSenderEmail(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SecureMessage> getMessages(Long threadId) {
        return messageRepo.findByThreadIdAndDeletedFalseOrderBySentAtAsc(threadId);
    }

    @Transactional
    public void markAsRead(Long messageId, String readerEmail) {
        receiptRepo.findByMessageIdAndReaderEmail(messageId, readerEmail).ifPresentOrElse(
                r -> {},
                () -> {
                    MessageReadReceipt receipt = MessageReadReceipt.builder()
                            .messageId(messageId)
                            .readerEmail(readerEmail)
                            .readAt(Instant.now())
                            .build();
                    receiptRepo.save(receipt);
                    messageRepo.findById(messageId).ifPresent(m -> {
                        m.setStatus(SecureMessage.MessageStatus.READ);
                        messageRepo.save(m);
                    });
                }
        );
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        messageRepo.findById(messageId).ifPresent(m -> {
            m.setDeleted(true);
            messageRepo.save(m);
        });
    }
}
