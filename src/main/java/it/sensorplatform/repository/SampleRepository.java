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

        @Query(value = """
                        SELECT
                            DATE_TRUNC(:period, s.timestamp) AS bucket,
                            m.key_name                       AS signalKey,
                            m.display_name                   AS displayName,
                            m.unit                           AS unit,
                            AVG(COALESCE(m.double_value, m.int_value::float)) AS avgVal,
                            MIN(COALESCE(m.double_value, m.int_value::float)) AS minVal,
                            MAX(COALESCE(m.double_value, m.int_value::float)) AS maxVal
                        FROM sample s
                        JOIN measurement_entity m ON m.sample_id = s.id
                        WHERE LOWER(REPLACE(s.device_id, ':', '')) = LOWER(REPLACE(:deviceId, ':', ''))
                          AND s.timestamp  >= :from
                          AND s.timestamp  <  :to
                          AND COALESCE(m.double_value, m.int_value::float) IS NOT NULL
                        GROUP BY bucket, m.key_name, m.display_name, m.unit
                        ORDER BY bucket ASC
                        """, nativeQuery = true)
        List<Object[]> findAggregated(
                        @Param("deviceId") String deviceId,
                        @Param("from") Instant from,
                        @Param("to") Instant to,
                        @Param("period") String period);
}
