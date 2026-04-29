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
        Instant bucket;
        if (row[0] instanceof java.sql.Timestamp ts) {
            bucket = ts.toInstant();
        } else if (row[0] instanceof Instant i) {
            bucket = i;
        } else {
            bucket = Instant.parse(row[0].toString());
        }
        return new AggregatedPointDTO(
                bucket,
                (String) row[1],
                (String) row[2],
                (String) row[3],
                row[4] != null ? ((Number) row[4]).doubleValue() : null,
                row[5] != null ? ((Number) row[5]).doubleValue() : null,
                row[6] != null ? ((Number) row[6]).doubleValue() : null);
    }
}