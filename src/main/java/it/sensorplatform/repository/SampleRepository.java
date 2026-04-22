package it.sensorplatform.repository;

import it.sensorplatform.model.SampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SampleRepository extends JpaRepository<SampleEntity, Long> {
    Optional<SampleEntity> findByDevEuiAndTimestamp(
            String devEui,
            Instant timestamp);

    @Query("SELECT s FROM SampleEntity s " +
            "WHERE s.deviceId = :deviceId " +
            "ORDER BY s.timestamp ASC")
    List<SampleEntity> findByDeviceIdOrderByTimestamp(
            @Param("deviceId") String deviceId);

    @Modifying
    @Query("DELETE FROM SampleEntity s " +
            "WHERE s.timestamp < :cutoff")
    void deleteOlderThan(
            @Param("cutoff") Instant cutoff);

}
