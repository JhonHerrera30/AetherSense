package it.sensorplatform.dto;

import it.sensorplatform.service.IngestService;

public record SpecDTO(String key,
                      String label,
                      String displayName,
                      String component,
                      String unit,
                      Double min,
                      Double max) {

    public static SpecDTO fromMeasurement(IngestService.MeasurementSample measurement) {
        if (measurement == null) {
            return new SpecDTO(null, null, null, null, null, null, null);
        }
        return new SpecDTO(
                measurement.key(),
                measurement.label(),
                measurement.displayName(),
                measurement.component(),
                measurement.unit(),
                measurement.min(),
                measurement.max()
        );
    }
}
