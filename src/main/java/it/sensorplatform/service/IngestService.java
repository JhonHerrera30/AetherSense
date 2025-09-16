package it.sensorplatform.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

    private static final List<String> SPEC_KEYS = List.of(
            "co2_ppm",
            "pm1p0",
            "pm2p5",
            "pm4p0",
            "pm10p0",
            "vocIndex",
            "noxIndex",
            "bmeT",
            "bmeRH",
            "bmeP",
            "bmeGas",
            "icmTemp",
            "icmAccX",
            "icmAccY",
            "icmAccZ",
            "icmGyrX",
            "icmGyrY",
            "icmGyrZ"
    );

    private static final List<String> INDICATOR_KEYS = List.of(
            "sen55_fan_err",
            "sen55_speed_warn",
            "sen55_laser_err",
            "sen55_rht_err",
            "sen55_gas_err",
            "sen55_cleaning"
    );

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

    /**
     * Normalizza e memorizza un pacchetto ricevuto.
     * @param deviceId nome del device (se null, prova a usare devEui)
     * @param devEui   EUI del device (opzionale, usato come fallback per l'id)
     * @param ts       istante della misura (UTC)
     * @param metrics  mappa chiave=tipo misura, valore=numero (es. temp, hum, pm2p5)
     */
    public void process(String deviceId,
                        String devEui,
                        Instant ts,
                        Map<String, Object> metrics,
                        List<it.sensorplatform.dto.PacketDTO.SpecEntry> specEntries,
                        List<String> indicatorLabels) {
        if (deviceId == null && devEui != null) {
            deviceId = devEui.toLowerCase(Locale.ROOT);
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId o devEui richiesto");
        }
        if (ts == null) ts = Instant.now();

        Map<String, Object> safeMetrics = metrics != null ? new HashMap<>(metrics) : new HashMap<>();

        List<MeasurementSample> measurements = buildMeasurements(safeMetrics, specEntries);
        List<IndicatorSample> indicators = buildIndicators(safeMetrics, indicatorLabels);
        Map<String, Object> info = extractInfo(safeMetrics);

        Deque<Sample> queue = store.computeIfAbsent(deviceId, k -> new ArrayDeque<>());
        queue.addLast(new Sample(deviceId, devEui, ts, measurements, indicators, Map.copyOf(info)));

        // mantieni dimensione massima
        while (queue.size() > MAX_SAMPLES_PER_DEVICE) {
            queue.removeFirst();
        }

        // TODO (Fase 2): persistere su DB
        // persistToDb(deviceId, devEui, ts, metrics);
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
                                                      List<it.sensorplatform.dto.PacketDTO.SpecEntry> specEntries) {
        List<MeasurementSample> result = new ArrayList<>();
        List<it.sensorplatform.dto.PacketDTO.SpecEntry> safeSpec = specEntries != null ? specEntries : List.of();
        for (int i = 0; i < SPEC_KEYS.size(); i++) {
            String key = SPEC_KEYS.get(i);
            Object rawValue = metrics.get(key);
            Double value = toDouble(rawValue);
            Double min = null;
            Double max = null;
            String label = key;
            if (i < safeSpec.size() && safeSpec.get(i) != null) {
                var entry = safeSpec.get(i);
                label = Optional.ofNullable(entry.getLabel()).orElse(key);
                min = entry.getMin();
                max = entry.getMax();
            }
            MeasurementMetadata metadata = MEASUREMENT_METADATA.get(key);
            String displayName = metadata != null ? metadata.displayName() : label;
            String unit = metadata != null ? metadata.unit() : null;
            result.add(new MeasurementSample(key, label, displayName, unit, min, max, value));
        }
        return result;
    }

    private List<IndicatorSample> buildIndicators(Map<String, Object> metrics, List<String> indicatorLabels) {
        List<IndicatorSample> result = new ArrayList<>();
        List<String> safeLabels = indicatorLabels != null ? indicatorLabels : List.of();
        for (int i = 0; i < INDICATOR_KEYS.size(); i++) {
            String key = INDICATOR_KEYS.get(i);
            Object rawValue = metrics.get(key);
            Integer value = toInteger(rawValue);
            String label = key;
            if (i < safeLabels.size() && safeLabels.get(i) != null) {
                label = safeLabels.get(i);
            } else if (INDICATOR_LABELS.containsKey(key)) {
                label = INDICATOR_LABELS.get(key);
            }
            result.add(new IndicatorSample(key, label, value));
        }
        return result;
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
