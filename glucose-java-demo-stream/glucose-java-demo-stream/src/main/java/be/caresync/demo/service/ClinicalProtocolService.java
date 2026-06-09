package be.caresync.demo.service;

import be.caresync.demo.model.db.careplan.ClinicalProtocol;
import be.caresync.demo.repository.ClinicalProtocolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClinicalProtocolService {

    private final ClinicalProtocolRepository protocolRepo;

    @Transactional(readOnly = true)
    public List<ClinicalProtocol> getAll() {
        return protocolRepo.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Optional<ClinicalProtocol> getById(Long id) {
        return protocolRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ClinicalProtocol> getByPathology(String pathology) {
        return protocolRepo.findByPathologyAndActiveTrue(pathology);
    }

    @Transactional
    public ClinicalProtocol create(ClinicalProtocol protocol) {
        protocol.setCreatedAt(Instant.now());
        return protocolRepo.save(protocol);
    }

    @Transactional
    public void deactivate(Long id) {
        protocolRepo.findById(id).ifPresent(p -> {
            p.setActive(false);
            protocolRepo.save(p);
        });
    }
}
