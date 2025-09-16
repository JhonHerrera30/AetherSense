package it.sensorplatform.dto;

import java.time.Instant;
import java.util.Map;

import it.sensorplatform.service.IngestService;

public record TelemetrySampleDTO(Instant timestamp, Map<String, Object> metrics) {

    public static TelemetrySampleDTO fromSample(IngestService.Sample sample) {
        if (sample == null) {
            return null;
        }
        return new TelemetrySampleDTO(sample.ts, sample.metrics);
    }
}

