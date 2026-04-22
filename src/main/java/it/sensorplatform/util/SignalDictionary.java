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
            boolean isIndicator) {
    }

    public static final Map<String, ChartConfig> SIGNALS = Map.ofEntries(
            // Temperatura
            Map.entry("temperature_celsius", new ChartConfig(
                    "gauge+line", "Temperatura", "°C", -40.0, 85.0, false)),

            // Umidità
            Map.entry("humidity_percent", new ChartConfig(
                    "gauge+line", "Umidità Relativa", "%RH", 0.0, 100.0, false)),

            // CO2
            Map.entry("co2concentration_ppm", new ChartConfig(
                    "gauge+line", "Anidride Carbonica", "ppm", 400.0, 10000.0, false)),

            // Pressione
            Map.entry("pressure_hpa", new ChartConfig(
                    "gauge+line", "Pressione Atmosferica", "hPa", 300.0, 1100.0, false)),

            // Gas resistance / IAQ
            Map.entry("gasresistance_ohm", new ChartConfig(
                    "gauge+line", "Qualità Aria (IAQ)", "Ω", 0.0, 500.0, false)),

            // VOC
            Map.entry("voc_index", new ChartConfig(
                    "gauge+line", "Composti Organici Volatili", "VOC", 1.0, 500.0, false)),

            // NOx
            Map.entry("nox_index", new ChartConfig(
                    "gauge+line", "Ossidi di Azoto", "NOx", 1.0, 500.0, false)),

            // Particolato
            Map.entry("pm1_0_ugm3", new ChartConfig(
                    "area+line", "Particolato PM1.0", "µg/m³", 0.0, 1000.0, false)),
            Map.entry("pm2_5_ugm3", new ChartConfig(
                    "area+line", "Particolato PM2.5", "µg/m³", 0.0, 1000.0, false)),
            Map.entry("pm4_0_ugm3", new ChartConfig(
                    "area+line", "Particolato PM4.0", "µg/m³", 0.0, 1000.0, false)),
            Map.entry("pm10_0_ugm3", new ChartConfig(
                    "area+line", "Particolato PM10", "µg/m³", 0.0, 1000.0, false)),

            // Sismico — misurazioni continue
            Map.entry("si_m_s", new ChartConfig(
                    "gauge+line", "Intensità Sismica (SI)", "m/s", 0.0, 100.0, false)),
            Map.entry("pga_m_s2", new ChartConfig(
                    "gauge+line", "Accelerazione di Picco (PGA)", "m/s²", 0.0, 20.0, false)),

            // Sismico — indicatori booleani
            Map.entry("earthquake_flag", new ChartConfig(
                    "boolean", "Terremoto Rilevato", "", 0.0, 1.0, true)),
            Map.entry("shutoff", new ChartConfig(
                    "boolean", "Shutoff", "", 0.0, 1.0, true)),
            Map.entry("collapse", new ChartConfig(
                    "boolean", "Collasso", "", 0.0, 1.0, true)),
            Map.entry("state", new ChartConfig(
                    "status", "Stato Sensore", "", 0.0, 7.0, true)),
            Map.entry("axis_state", new ChartConfig(
                    "status", "Stato Asse", "", 0.0, 3.0, true)));

    /**
     * Restituisce la configurazione per un segnale.
     * Se il segnale non è nel dizionario, usa un fallback generico.
     * Se il database ha min/max specifici, li usa al posto dei default.
     */
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

        // se il database ha min/max specifici sovrascrive i default
        if (minFromDb != null && maxFromDb != null) {
            return new ChartConfig(
                    config.chartType(),
                    config.displayName(),
                    config.unit(),
                    minFromDb,
                    maxFromDb,
                    config.isIndicator());
        }
        return config;
    }

    /**
     * Controlla se un segnale è un indicatore booleano/discreto.
     * Usato dall'IngestService per classificare MEASUREMENT vs INDICATOR.
     */
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