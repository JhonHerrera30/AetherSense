package it.sensorplatform.dto;

import it.sensorplatform.service.IngestService;
import java.util.Map;

public record SpecDTO(String key,
        String label,
        String displayName,
        String component,
        String unit,
        Double min,
        Double max,
        Map<Integer, String> stateLabels) {

    // costruttore di compatibilità senza stateLabels
    public SpecDTO(String key, String label, String displayName,
            String component, String unit, Double min, Double max) {
        this(key, label, displayName, component, unit, min, max, Map.of());
    }

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
                measurement.max());
    }

    public static SpecDTO fromMeasurementEntity(
            it.sensorplatform.model.MeasurementEntity m) {
        if (m == null)
            return new SpecDTO(null, null, null, null, null, null, null);
        return new SpecDTO(
                m.getKey(), m.getLabel(), m.getDisplayName(),
                m.getComponent(), m.getUnit(),
                m.getMin(), m.getMax());
    }
}