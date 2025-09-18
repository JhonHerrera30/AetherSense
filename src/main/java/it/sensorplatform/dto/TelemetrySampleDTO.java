package it.sensorplatform.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import it.sensorplatform.service.IngestService;

public record TelemetrySampleDTO(Instant timestamp,
                                 List<MeasurementDTO> measurements,
                                 List<IndicatorDTO> indicators,
                                 Map<String, Object> info) {

    public static TelemetrySampleDTO fromSample(IngestService.Sample sample) {
        if (sample == null) {
            return null;
        }
        List<MeasurementDTO> measurementDTOs = sample.measurements().stream()
                .map(MeasurementDTO::fromMeasurement)
                .collect(Collectors.toList());
        List<IndicatorDTO> indicatorDTOs = sample.indicators().stream()
                .map(IndicatorDTO::fromIndicator)
                .collect(Collectors.toList());
        return new TelemetrySampleDTO(sample.ts(), measurementDTOs, indicatorDTOs, sample.info());
    }

    public record MeasurementDTO(String key,
                                 String label,
                                 String displayName,
                                 String component,
                                 String unit,
                                 Double min,
                                 Double max,
                                 Double value) {

        private static MeasurementDTO fromMeasurement(IngestService.MeasurementSample measurement) {
            return new MeasurementDTO(
                    measurement.key(),
                    measurement.label(),
                    measurement.displayName(),
                    measurement.component(),
                    measurement.unit(),
                    measurement.min(),
                    measurement.max(),
                    measurement.value()
            );
        }
    }

    public record IndicatorDTO(String key, String label, Integer value) {
        private static IndicatorDTO fromIndicator(IngestService.IndicatorSample indicator) {
            return new IndicatorDTO(indicator.key(), indicator.label(), indicator.value());
        }
    }
}
