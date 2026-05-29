package it.sensorplatform.util;

import java.util.Map;

public class AlertDefaults {

    public record Thresholds(
            Double warningHigh,
            Double criticalHigh,
            Double warningLow,
            Double criticalLow) {

        // solo high
        public static Thresholds onlyHigh(double warning, double critical) {
            return new Thresholds(warning, critical, null, null);
        }

        // solo low
        public static Thresholds onlyLow(double warning, double critical) {
            return new Thresholds(null, null, warning, critical);
        }

        // entrambi
        public static Thresholds both(double wHigh, double cHigh, double wLow, double cLow) {
            return new Thresholds(wHigh, cHigh, wLow, cLow);
        }
    }

    // chiave: "projectKey:signalKey" oppure "default:signalKey"
    private static final Map<String, Thresholds> DEFAULTS = Map.ofEntries(

            // ── TEMPERATURA ─────────────────────────────────────────────────────
            // LTRAD (lampioni): sia troppo caldo che troppo freddo è problema
            Map.entry("ltrad:temperature_celsius", Thresholds.both(45.0, 60.0, -5.0, -20.0)),
            // FIRE (incendi): solo alto è problema, basse temperature non rilevanti
            Map.entry("fire:temperature_celsius", Thresholds.onlyHigh(38.0, 55.0)),
            // VOLCANO (vulcani): soglie più alte per ambiente naturalmente caldo
            Map.entry("volcano:temperature_celsius", Thresholds.both(55.0, 75.0, -5.0, -20.0)),

            // ── UMIDITÀ ─────────────────────────────────────────────────────────
            // LTRAD: troppo umido o troppo secco può danneggiare l'elettronica
            Map.entry("ltrad:humidity_percent", Thresholds.both(80.0, 90.0, 20.0, 10.0)),
            // FIRE: umidità bassa = rischio incendio alto
            Map.entry("fire:humidity_percent", Thresholds.both(75.0, 85.0, 25.0, 15.0)),
            // VOLCANO: range più ampio tollerato
            Map.entry("volcano:humidity_percent", Thresholds.both(85.0, 95.0, 15.0, 5.0)),

            // ── CO₂ ─────────────────────────────────────────────────────────────
            // uguale per tutti: valori OMS come riferimento
            // 400 ppm normale, 1000 scarsa ventilazione, 2000+ dannoso
            Map.entry("default:co2concentration_ppm", Thresholds.onlyHigh(1000.0, 2000.0)),

            // ── PRESSIONE ───────────────────────────────────────────────────────
            // pressione normale 1013 hPa, variazioni significative = fenomeni meteo
            Map.entry("ltrad:pressure_hpa", Thresholds.both(1030.0, 1040.0, 980.0, 960.0)),
            Map.entry("fire:pressure_hpa", Thresholds.both(1035.0, 1045.0, 975.0, 955.0)),
            Map.entry("volcano:pressure_hpa", Thresholds.both(1040.0, 1055.0, 960.0, 940.0)),

            // ── QUALITÀ ARIA (gas resistance) ───────────────────────────────────
            // alto = aria pulita, basso = aria inquinata (logica invertita)
            Map.entry("default:gasresistance_ohm", Thresholds.onlyLow(50000.0, 20000.0)),

            // ── VOC INDEX ───────────────────────────────────────────────────────
            // 0-100 normale, 100-150 scarso, 150+ cattivo
            Map.entry("default:voc_index", Thresholds.onlyHigh(150.0, 250.0)),

            // ── NOX INDEX ───────────────────────────────────────────────────────
            // simile a VOC
            Map.entry("default:nox_index", Thresholds.onlyHigh(150.0, 250.0)),

            // ── PARTICOLATO ─────────────────────────────────────────────────────
            // OMS: PM2.5 > 25 µg/m³ cattivo, > 50 molto cattivo
            Map.entry("default:pm1_0_ugm3", Thresholds.onlyHigh(25.0, 50.0)),
            Map.entry("default:pm2_5_ugm3", Thresholds.onlyHigh(25.0, 50.0)),
            Map.entry("default:pm4_0_ugm3", Thresholds.onlyHigh(35.0, 70.0)),
            Map.entry("default:pm10_0_ugm3", Thresholds.onlyHigh(50.0, 100.0)),

            // ── SISMICA SI ──────────────────────────────────────────────────────
            // SI in m/s: < 0.1 impercettibile, 0.1-0.5 leggero, > 1.0 forte
            Map.entry("default:si_m_s", Thresholds.onlyHigh(0.1, 0.5)),

            // ── PGA ─────────────────────────────────────────────────────────────
            // PGA in m/s²: < 0.05 impercettibile, 0.05-0.3 leggero, > 1.0 forte
            Map.entry("default:pga_m_s2", Thresholds.onlyHigh(0.05, 0.3)));

    /**
     * Restituisce le soglie di default per un segnale.
     * Prima cerca per progetto specifico, poi fallback a "default".
     */
    public static Thresholds get(String projectKey, String signalKey) {
        if (projectKey != null && signalKey != null) {
            Thresholds specific = DEFAULTS.get(
                    projectKey.toLowerCase() + ":" + signalKey.toLowerCase());
            if (specific != null)
                return specific;
        }
        if (signalKey != null) {
            Thresholds fallback = DEFAULTS.get("default:" + signalKey.toLowerCase());
            if (fallback != null)
                return fallback;
        }
        return null;
    }

    // compatibilità con vecchio formato double[] {warning, critical}
    public static double[] getAsArray(String signalKey) {
        Thresholds t = get("default", signalKey);
        if (t == null)
            return null;
        double w = t.warningHigh() != null ? t.warningHigh()
                : (t.warningLow() != null ? t.warningLow() : 0);
        double c = t.criticalHigh() != null ? t.criticalHigh()
                : (t.criticalLow() != null ? t.criticalLow() : 0);
        return new double[] { w, c };
    }
}