package it.sensorplatform.repository;

import it.sensorplatform.model.AlertConfigGlobal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AlertConfigGlobalRepository
        extends JpaRepository<AlertConfigGlobal, Long> {
    Optional<AlertConfigGlobal> findByProjectId(Long projectId);
}