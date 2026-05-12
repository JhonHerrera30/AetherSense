package it.sensorplatform.repository;

import it.sensorplatform.model.AlertConfigSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertConfigSignalRepository
        extends JpaRepository<AlertConfigSignal, Long> {
    List<AlertConfigSignal> findByProjectId(Long projectId);

    Optional<AlertConfigSignal> findByProjectIdAndSignalKey(Long projectId, String signalKey);

    void deleteByProjectId(Long projectId);
}