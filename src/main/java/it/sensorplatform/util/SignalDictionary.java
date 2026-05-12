package it.sensorplatform.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SignalDictionary {

        public record ChartConfig(
                        String chartType,
                        String displayName,
                        String unit,
                        double defaultMin,
                        double defaultMax,
                        boolean isIndicator,
                        Map<Integer, String> stateLabels) {

                public ChartConfig(String chartType, String displayName, String unit,
                                double defaultMin, double defaultMax, boolean isIndicator) {
                        this(chartType, displayName, unit, defaultMin, defaultMax, isIndicator, Map.of());
                }
        }

        public static final Map<String, ChartConfig> SIGNALS = Map.ofEntries(
                        Map.entry("temperature_celsius", new ChartConfig(
                                        "gauge+line", "Temperature", "°C", -40.0, 85.0, false)),
                        Map.entry("humidity_percent", new ChartConfig(
                                        "gauge+line", "Relative Humidity", "%RH", 0.0, 100.0, false)),
                        Map.entry("co2concentration_ppm", new ChartConfig(
                                        "gauge+line", "CO₂ Concentration", "ppm", 400.0, 10000.0, false)),
                        Map.entry("pressure_hpa", new ChartConfig(
                                        "gauge+line", "Atmospheric Pressure", "hPa", 300.0, 1100.0, false)),
                        Map.entry("gasresistance_ohm", new ChartConfig(
                                        "gauge+line", "Air Quality (IAQ)", "Ω", 0.0, 500.0, false)),
                        Map.entry("voc_index", new ChartConfig(
                                        "gauge+line", "VOC Index", "VOC", 1.0, 500.0, false)),
                        Map.entry("nox_index", new ChartConfig(
                                        "gauge+line", "NOx Index", "NOx", 1.0, 500.0, false)),
                        Map.entry("pm1_0_ugm3", new ChartConfig(
                                        "area+line", "PM1.0 Particulate", "µg/m³", 0.0, 1000.0, false)),
                        Map.entry("pm2_5_ugm3", new ChartConfig(
                                        "area+line", "PM2.5 Particulate", "µg/m³", 0.0, 1000.0, false)),
                        Map.entry("pm4_0_ugm3", new ChartConfig(
                                        "area+line", "PM4.0 Particulate", "µg/m³", 0.0, 1000.0, false)),
                        Map.entry("pm10_0_ugm3", new ChartConfig(
                                        "area+line", "PM10 Particulate", "µg/m³", 0.0, 1000.0, false)),
                        Map.entry("si_m_s", new ChartConfig(
                                        "gauge+line", "Seismic Intensity (SI)", "m/s", 0.0, 100.0, false)),
                        Map.entry("pga_m_s2", new ChartConfig(
                                        "gauge+line", "Peak Ground Acceleration", "m/s²", 0.0, 20.0, false)),
                        Map.entry("earthquake_flag", new ChartConfig(
                                        "boolean", "Earthquake Detected", "", 0.0, 1.0, true)),
                        Map.entry("shutoff", new ChartConfig(
                                        "boolean", "Shutoff", "", 0.0, 1.0, true)),
                        Map.entry("collapse", new ChartConfig(
                                        "boolean", "Collapse", "", 0.0, 1.0, true)),
                        Map.entry("state", new ChartConfig(
                                        "status", "Sensor State", "", 0.0, 7.0, true,
                                        Map.of(0, "OK", 1, "Initializing", 2, "Warning",
                                                        3, "Error", 4, "Critical", 5, "Maintenance",
                                                        6, "Offline", 7, "Fault"))),
                        Map.entry("axis_state", new ChartConfig(
                                        "status", "Axis State", "", 0.0, 3.0, true,
                                        Map.of(0, "OK", 1, "Tilted", 2, "Warning", 3, "Critical"))));

        public static ChartConfig getConfig(String signalKey,
                        Double minFromDb,
                        Double maxFromDb) {
                String normalizedKey = signalKey != null
                                ? signalKey.toLowerCase(Locale.ROOT)
                                : "";

                ChartConfig config = SIGNALS.getOrDefault(
                                normalizedKey,
                                new ChartConfig(
                                                "gauge+line",
                                                prettify(signalKey),
                                                "",
                                                0.0, 100.0,
                                                false));

                if (minFromDb != null && maxFromDb != null) {
                        return new ChartConfig(
                                        config.chartType(),
                                        config.displayName(),
                                        config.unit(),
                                        minFromDb,
                                        maxFromDb,
                                        config.isIndicator(),
                                        config.stateLabels());
                }
                return config;
        }

        public static boolean isIndicator(String signalKey) {
                if (signalKey == null)
                        return false;
                String normalized = signalKey.toLowerCase(Locale.ROOT);
                ChartConfig config = SIGNALS.get(normalized);
                return config != null && config.isIndicator();
        }

        private static String prettify(String key) {
                if (key == null)
                        return "";
                return Arrays.stream(key.split("[_\\-]"))
                                .map(w -> w.isEmpty() ? w : w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1))
                                .collect(Collectors.joining(" "));
        }
}