package be.caresync.demo.repository;

import be.caresync.demo.model.db.rgpd.DataRetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DataRetentionPolicyRepository extends JpaRepository<DataRetentionPolicy, Long> {
    Optional<DataRetentionPolicy> findByDataCategory(String dataCategory);
    List<DataRetentionPolicy> findByAutoPurgeTrue();
}
