package it.sensorplatform.service;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Spec;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IngestService: punto unico che riceve dati normalizzati (deviceId/devEui, timestamp, metrics)
 * e li memorizza in un buffer in-memory per i test locali.
 *
 * Fase 2 (quando vorrai): qui dentro collegheremo i repository per persistere su DB.
 */
@Service
public class IngestService {

    /** Record di un singolo campione ricevuto */
    public static record Sample(String deviceId,
                                String devEui,
                                Instant ts,
                                List<MeasurementSample> measurements,
                                List<IndicatorSample> indicators,
                                Map<String, Object> info) { }

    public static record MeasurementSample(String key,
                                           String label,
                                           String displayName,
                                           String unit,
                                           Double min,
                                           Double max,
                                           Double value) { }

    public static record IndicatorSample(String key,
                                         String label,
                                         Integer value) { }

    /** Ultimi N campioni per device (buffer circolare) */
    private final Map<String, Deque<Sample>> store = new ConcurrentHashMap<>();
    private static final int MAX_SAMPLES_PER_DEVICE = 200;

    private static final Map<String, MeasurementMetadata> MEASUREMENT_METADATA = Map.ofEntries(
            Map.entry("co2_ppm", new MeasurementMetadata("CO₂", "ppm")),
            Map.entry("pm1p0", new MeasurementMetadata("PM1.0", "µg/m³")),
            Map.entry("pm2p5", new MeasurementMetadata("PM2.5", "µg/m³")),
            Map.entry("pm4p0", new MeasurementMetadata("PM4.0", "µg/m³")),
            Map.entry("pm10p0", new MeasurementMetadata("PM10", "µg/m³")),
            Map.entry("vocIndex", new MeasurementMetadata("VOC Index", "index")),
            Map.entry("noxIndex", new MeasurementMetadata("NOx Index", "index")),
            Map.entry("bmeT", new MeasurementMetadata("BME680 Temperature", "°C")),
            Map.entry("bmeRH", new MeasurementMetadata("BME680 Humidity", "%")),
            Map.entry("bmeP", new MeasurementMetadata("BME680 Pressure", "Pa")),
            Map.entry("bmeGas", new MeasurementMetadata("BME680 Gas", "Ω")),
            Map.entry("icmTemp", new MeasurementMetadata("ICM-20948 Temperature", "°C")),
            Map.entry("icmAccX", new MeasurementMetadata("ICM-20948 AccX", "g")),
            Map.entry("icmAccY", new MeasurementMetadata("ICM-20948 AccY", "g")),
            Map.entry("icmAccZ", new MeasurementMetadata("ICM-20948 AccZ", "g")),
            Map.entry("icmGyrX", new MeasurementMetadata("ICM-20948 GyrX", "°/s")),
            Map.entry("icmGyrY", new MeasurementMetadata("ICM-20948 GyrY", "°/s")),
            Map.entry("icmGyrZ", new MeasurementMetadata("ICM-20948 GyrZ", "°/s"))
    );

    private static final Map<String, String> INDICATOR_LABELS = Map.of(
            "sen55_fan_err", "SEN55 Fan Error",
            "sen55_speed_warn", "SEN55 Speed Warning",
            "sen55_laser_err", "SEN55 Laser Error",
            "sen55_rht_err", "SEN55 RHT Error",
            "sen55_gas_err", "SEN55 Gas Error",
            "sen55_cleaning", "SEN55 Cleaning"
    );

    private static final Set<String> INFO_KEYS = Set.of(
            "currentDate",
            "currentTime",
            "macAddress",
            "bat_V",
            "bat_pct",
            "latitude",
            "longitude"
    );

    private record MeasurementMetadata(String displayName, String unit) { }

    private record IndicatorDescriptor(String key, String label) { }

    /**
     * Normalizza e memorizza un pacchetto ricevuto.
     * @param deviceId nome del device (se null, prova a usare devEui)
     * @param devEui   EUI del device (opzionale, usato come fallback per l'id)
     * @param ts       istante della misura (UTC)
     * @param metrics  mappa chiave=tipo misura, valore=numero (es. temp, hum, pm2p5)
     */
    public void process(Device device,
                        Instant ts,
                        Map<String, Object> metrics,
                        List<PacketDTO.SpecEntry> specEntries,
                        List<String> indicatorLabels) {
        String deviceId = device != null ? device.getMacAddress() : null;
        String devEui = device != null ? device.getDevEui() : null;
        List<Spec> savedSpecs = device != null && device.getTod() != null && device.getTod().getSpecs() != null
                ? device.getTod().getSpecs()
                : List.of();
        process(deviceId, devEui, ts, metrics, specEntries, indicatorLabels, savedSpecs);
    }

    public void process(String deviceId,
                        String devEui,
                        Instant ts,
                        Map<String, Object> metrics,
                        List<PacketDTO.SpecEntry> specEntries,
                        List<String> indicatorLabels) {
        process(deviceId, devEui, ts, metrics, specEntries, indicatorLabels, List.of());
    }

    private void process(String deviceId,
                         String devEui,
                         Instant ts,
                         Map<String, Object> metrics,
                         List<PacketDTO.SpecEntry> specEntries,
                         List<String> indicatorLabels,
                         List<Spec> savedSpecs) {
        if (deviceId == null && devEui != null) {
            deviceId = devEui.toLowerCase(Locale.ROOT);
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId o devEui richiesto");
        }
        if (ts == null) ts = Instant.now();

        Map<String, Object> safeMetrics = metrics != null ? new LinkedHashMap<>(metrics) : new LinkedHashMap<>();
        List<PacketDTO.SpecEntry> safeSpecEntries = specEntries != null ? specEntries : List.of();
        List<Spec> safeSavedSpecs = savedSpecs != null ? savedSpecs : List.of();

        List<IndicatorDescriptor> indicatorDescriptors = parseIndicatorDescriptors(indicatorLabels);
        Set<String> indicatorKeyHints = new LinkedHashSet<>();
        for (IndicatorDescriptor descriptor : indicatorDescriptors) {
            if (descriptor != null && descriptor.key() != null) {
                indicatorKeyHints.add(descriptor.key());
            }
        }

        List<MeasurementSample> measurements = buildMeasurements(safeMetrics, safeSpecEntries, safeSavedSpecs, indicatorKeyHints);
        Set<String> measurementKeys = new LinkedHashSet<>();
        for (MeasurementSample measurement : measurements) {
            measurementKeys.add(measurement.key());
        }
        List<IndicatorSample> indicators = buildIndicators(safeMetrics, indicatorDescriptors, measurementKeys);
        Map<String, Object> info = extractInfo(safeMetrics);

        Deque<Sample> queue = store.computeIfAbsent(deviceId, k -> new ArrayDeque<>());
        queue.addLast(new Sample(deviceId, devEui, ts, measurements, indicators, Map.copyOf(info)));

        while (queue.size() > MAX_SAMPLES_PER_DEVICE) {
            queue.removeFirst();
        }
    }

    /**
     * Ritorna gli ultimi n campioni memorizzati per un device.
     */
    public List<Sample> last(String deviceId, int n) {
        Deque<Sample> q = store.getOrDefault(deviceId, new ArrayDeque<>());
        if (n <= 0 || q.isEmpty()) return List.of();
        int skip = Math.max(0, q.size() - n);
        return q.stream().skip(skip).toList();
    }

    private List<MeasurementSample> buildMeasurements(Map<String, Object> metrics,
                                                      List<PacketDTO.SpecEntry> specEntries,
                                                      List<Spec> savedSpecs,
                                                      Set<String> indicatorKeyHints) {
        List<MeasurementSample> result = new ArrayList<>();
        List<PacketDTO.SpecEntry> safeSpecEntries = specEntries != null ? specEntries : List.of();
        List<Spec> safeSavedSpecs = savedSpecs != null ? savedSpecs : List.of();

        Map<String, PacketDTO.SpecEntry> specByKey = indexSpecEntries(safeSpecEntries);
        Map<String, Spec> savedSpecByKey = indexSavedSpecs(safeSavedSpecs);
        List<String> measurementKeys = resolveMeasurementKeys(metrics, safeSpecEntries, safeSavedSpecs, indicatorKeyHints);

        for (int i = 0; i < measurementKeys.size(); i++) {
            String key = measurementKeys.get(i);
            Object rawValue = metrics.get(key);
            Double value = toDouble(rawValue);

            PacketDTO.SpecEntry specEntry = specByKey.get(key);
            if (specEntry == null && i < safeSpecEntries.size()) {
                specEntry = safeSpecEntries.get(i);
            }
            Spec savedSpec = savedSpecByKey.get(key);

            Double min = specEntry != null ? specEntry.getMin() : null;
            Double max = specEntry != null ? specEntry.getMax() : null;
            String label = resolveMeasurementLabel(key, specEntry, savedSpec);
            MeasurementMetadata metadata = MEASUREMENT_METADATA.get(key);
            String displayName = resolveMeasurementDisplayName(key, label, metadata, savedSpec);
            String unit = resolveMeasurementUnit(metadata, savedSpec);

            result.add(new MeasurementSample(key, label, displayName, unit, min, max, value));
        }
        return result;
    }

    private List<IndicatorSample> buildIndicators(Map<String, Object> metrics,
                                                  List<IndicatorDescriptor> indicatorDescriptors,
                                                  Set<String> measurementKeys) {
        List<IndicatorSample> result = new ArrayList<>();
        List<IndicatorDescriptor> safeDescriptors = indicatorDescriptors != null ? indicatorDescriptors : List.of();
        List<String> indicatorKeys = resolveIndicatorKeys(metrics, safeDescriptors, measurementKeys);
        for (int i = 0; i < indicatorKeys.size(); i++) {
            String key = indicatorKeys.get(i);
            Object rawValue = metrics.get(key);
            Integer value = toInteger(rawValue);
            String label = resolveIndicatorLabel(key, safeDescriptors, i);
            result.add(new IndicatorSample(key, label, value));
        }
        return result;
    }

    private Map<String, PacketDTO.SpecEntry> indexSpecEntries(List<PacketDTO.SpecEntry> specEntries) {
        Map<String, PacketDTO.SpecEntry> map = new HashMap<>();
        if (specEntries == null) {
            return map;
        }
        for (PacketDTO.SpecEntry entry : specEntries) {
            if (entry == null) {
                continue;
            }
            String key = sanitize(entry.getKey());
            if (key != null && !map.containsKey(key)) {
                map.put(key, entry);
            }
        }
        return map;
    }

    private Map<String, Spec> indexSavedSpecs(List<Spec> specs) {
        Map<String, Spec> map = new HashMap<>();
        if (specs == null) {
            return map;
        }
        for (Spec spec : specs) {
            if (spec == null) {
                continue;
            }
            String key = sanitize(spec.getMeasurement());
            if (key != null && !map.containsKey(key)) {
                map.put(key, spec);
            }
        }
        return map;
    }

    private List<String> resolveMeasurementKeys(Map<String, Object> metrics,
                                                List<PacketDTO.SpecEntry> specEntries,
                                                List<Spec> savedSpecs,
                                                Set<String> indicatorKeyHints) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (savedSpecs != null) {
            for (Spec spec : savedSpecs) {
                if (spec == null) {
                    continue;
                }
                String key = sanitize(spec.getMeasurement());
                if (key != null) {
                    keys.add(key);
                }
            }
        }
        if (specEntries != null) {
            for (PacketDTO.SpecEntry entry : specEntries) {
                if (entry == null) {
                    continue;
                }
                String key = sanitize(entry.getKey());
                if (key != null) {
                    keys.add(key);
                }
            }
        }
        if (metrics != null) {
            for (String rawKey : metrics.keySet()) {
                String key = sanitize(rawKey);
                if (key == null) {
                    continue;
                }
                if (INFO_KEYS.contains(key)) {
                    continue;
                }
                if (indicatorKeyHints != null && indicatorKeyHints.contains(key)) {
                    continue;
                }
                keys.add(key);
            }
        }
        return new ArrayList<>(keys);
    }

    private String resolveMeasurementLabel(String key, PacketDTO.SpecEntry specEntry, Spec savedSpec) {
        if (specEntry != null) {
            String label = sanitize(specEntry.getLabel());
            if (label != null) {
                return label;
            }
        }
        if (savedSpec != null) {
            String component = sanitize(savedSpec.getComponent());
            String measurement = sanitize(savedSpec.getMeasurement());
            if (component != null && measurement != null) {
                if (component.equalsIgnoreCase(measurement)) {
                    return component;
                }
                return component + " - " + measurement;
            }
            if (component != null) {
                return component;
            }
            if (measurement != null) {
                return measurement;
            }
        }
        return key;
    }

    private String resolveMeasurementDisplayName(String key,
                                                 String label,
                                                 MeasurementMetadata metadata,
                                                 Spec savedSpec) {
        if (metadata != null && metadata.displayName() != null) {
            return metadata.displayName();
        }
        if (savedSpec != null) {
            String measurement = sanitize(savedSpec.getMeasurement());
            if (measurement != null) {
                String pretty = prettify(measurement);
                if (pretty != null) {
                    return pretty;
                }
            }
            String component = sanitize(savedSpec.getComponent());
            if (component != null) {
                String pretty = prettify(component);
                if (pretty != null) {
                    return pretty;
                }
            }
        }
        if (label != null) {
            return label;
        }
        return prettify(key);
    }

    private String resolveMeasurementUnit(MeasurementMetadata metadata, Spec savedSpec) {
        if (metadata != null && metadata.unit() != null) {
            return metadata.unit();
        }
        if (savedSpec != null) {
            String unit = sanitize(savedSpec.getUnitOfMeasurement());
            if (unit != null) {
                return unit;
            }
        }
        return null;
    }

    private List<IndicatorDescriptor> parseIndicatorDescriptors(List<String> indicatorLabels) {
        if (indicatorLabels == null || indicatorLabels.isEmpty()) {
            return List.of();
        }
        List<IndicatorDescriptor> descriptors = new ArrayList<>();
        for (String raw : indicatorLabels) {
            if (raw == null) {
                descriptors.add(new IndicatorDescriptor(null, null));
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                descriptors.add(new IndicatorDescriptor(null, null));
                continue;
            }
            int separator = findSeparator(trimmed);
            if (separator > 0) {
                String keyPart = trimmed.substring(0, separator).trim();
                String labelPart = trimmed.substring(separator + 1).trim();
                descriptors.add(new IndicatorDescriptor(sanitize(keyPart), labelPart.isEmpty() ? null : labelPart));
            } else {
                descriptors.add(new IndicatorDescriptor(null, trimmed));
            }
        }
        return descriptors;
    }

    private List<String> resolveIndicatorKeys(Map<String, Object> metrics,
                                              List<IndicatorDescriptor> indicatorDescriptors,
                                              Set<String> measurementKeys) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (indicatorDescriptors != null) {
            for (IndicatorDescriptor descriptor : indicatorDescriptors) {
                if (descriptor == null) {
                    continue;
                }
                String key = sanitize(descriptor.key());
                if (key != null) {
                    keys.add(key);
                }
            }
        }
        if (metrics != null) {
            for (String rawKey : metrics.keySet()) {
                String key = sanitize(rawKey);
                if (key == null) {
                    continue;
                }
                if (INFO_KEYS.contains(key)) {
                    continue;
                }
                if (measurementKeys != null && measurementKeys.contains(key)) {
                    continue;
                }
                keys.add(key);
            }
        }
        LinkedHashSet<String> remaining = new LinkedHashSet<>(keys);
        List<String> ordered = new ArrayList<>();
        if (indicatorDescriptors != null) {
            for (IndicatorDescriptor descriptor : indicatorDescriptors) {
                if (descriptor == null) {
                    continue;
                }
                String key = sanitize(descriptor.key());
                if (key != null && remaining.remove(key)) {
                    ordered.add(key);
                } else if (key == null && !remaining.isEmpty()) {
                    String next = remaining.iterator().next();
                    remaining.remove(next);
                    ordered.add(next);
                }
            }
        }
        ordered.addAll(remaining);
        return ordered;
    }

    private String resolveIndicatorLabel(String key,
                                         List<IndicatorDescriptor> descriptors,
                                         int index) {
        if (descriptors != null && !descriptors.isEmpty()) {
            if (index < descriptors.size()) {
                IndicatorDescriptor descriptor = descriptors.get(index);
                if (descriptor != null) {
                    String descriptorKey = sanitize(descriptor.key());
                    String descriptorLabel = descriptor.label();
                    if ((descriptorKey == null || descriptorKey.equals(key)) && descriptorLabel != null && !descriptorLabel.isBlank()) {
                        return descriptorLabel;
                    }
                }
            }
            for (IndicatorDescriptor descriptor : descriptors) {
                if (descriptor == null) {
                    continue;
                }
                String descriptorKey = sanitize(descriptor.key());
                if (descriptorKey != null && descriptorKey.equals(key)) {
                    String descriptorLabel = descriptor.label();
                    if (descriptorLabel != null && !descriptorLabel.isBlank()) {
                        return descriptorLabel;
                    }
                }
            }
        }
        if (INDICATOR_LABELS.containsKey(key)) {
            return INDICATOR_LABELS.get(key);
        }
        String pretty = prettify(key);
        return pretty != null ? pretty : key;
    }

    private int findSeparator(String value) {
        int colon = value.indexOf(':');
        int equal = value.indexOf('=');
        int pipe = value.indexOf('|');
        int separator = colon > 0 ? colon : -1;
        if (separator < 0 || (equal > 0 && equal < separator)) {
            separator = equal;
        }
        if (separator < 0 || (pipe > 0 && pipe < separator)) {
            separator = pipe;
        }
        return separator;
    }

    private String prettify(String value) {
        String sanitized = sanitize(value);
        if (sanitized == null) {
            return null;
        }
        String replaced = sanitized.replace('_', ' ').replace('-', ' ').trim();
        if (replaced.isEmpty()) {
            return sanitized;
        }
        String[] parts = replaced.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() > 0 ? builder.toString() : sanitized;
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> extractInfo(Map<String, Object> metrics) {
        Map<String, Object> info = new LinkedHashMap<>();
        if (metrics == null || metrics.isEmpty()) {
            return info;
        }
        for (String key : INFO_KEYS) {
            if (metrics.containsKey(key)) {
                info.put(key, metrics.get(key));
            }
        }
        return info;
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
