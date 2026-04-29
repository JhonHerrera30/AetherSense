package it.sensorplatform.dto;

import java.time.Instant;

public record AggregatedPointDTO(
        Instant bucket,
        String signalKey,
        String displayName,
        String unit,
        Double avg,
        Double min,
        Double max) {

    public static AggregatedPointDTO fromRow(Object[] row) {
        return new AggregatedPointDTO(
                ((java.sql.Timestamp) row[0]).toInstant(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                row[4] != null ? ((Number) row[4]).doubleValue() : null,
                row[5] != null ? ((Number) row[5]).doubleValue() : null,
                row[6] != null ? ((Number) row[6]).doubleValue() : null);
    }
}