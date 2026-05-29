package it.sensorplatform.service;

import it.sensorplatform.model.AlertConfigSignal;
import it.sensorplatform.model.AlertConfigGlobal;
import it.sensorplatform.model.Device;
import it.sensorplatform.repository.AlertConfigSignalRepository;
import it.sensorplatform.repository.AlertConfigGlobalRepository;
import it.sensorplatform.service.IngestService.MeasurementSample;
import it.sensorplatform.service.IngestService.IndicatorSample;
import it.sensorplatform.util.SignalDictionary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AlertService {

    private static final int DEFAULT_INTERVAL_MIN = 30;

    private static final Map<String, double[]> SIGNAL_DEFAULT_THRESHOLDS = Map.ofEntries(
            Map.entry("temperature_celsius", new double[] { -28.75, 72.25 }),
            Map.entry("humidity_percent", new double[] { 15.0, 85.0 }),
            Map.entry("co2concentration_ppm", new double[] { 1920.0, 4800.0 }),
            Map.entry("pressure_hpa", new double[] { 500.0, 990.0 }),
            Map.entry("gasresistance_ohm", new double[] { 150.0, 300.0 }),
            Map.entry("voc_index", new double[] { 149.3, 299.0 }),
            Map.entry("nox_index", new double[] { 149.3, 299.0 }),
            Map.entry("pm1_0_ugm3", new double[] { 200.0, 500.0 }),
            Map.entry("pm2_5_ugm3", new double[] { 200.0, 500.0 }),
            Map.entry("pm4_0_ugm3", new double[] { 200.0, 500.0 }),
            Map.entry("pm10_0_ugm3", new double[] { 200.0, 500.0 }),
            Map.entry("si_m_s", new double[] { 15.0, 40.0 }),
            Map.entry("pga_m_s2", new double[] { 3.0, 8.0 }));

    private final AlertConfigSignalRepository signalRepo;
    private final AlertConfigGlobalRepository globalRepo;
    private final TelegramBotService telegramBotService;

    public AlertService(AlertConfigSignalRepository signalRepo,
            AlertConfigGlobalRepository globalRepo,
            TelegramBotService telegramBotService) {
        this.signalRepo = signalRepo;
        this.globalRepo = globalRepo;
        this.telegramBotService = telegramBotService;
    }

    public void evaluate(Device device,
            List<MeasurementSample> measurements,
            List<IndicatorSample> indicators) {
        if (device == null || device.getProject() == null)
            return;

        Long projectId = device.getProject().getId();
        AlertConfigGlobal global = globalRepo.findByProjectId(projectId).orElse(null);
        String chatId = global != null ? global.getTelegramChatId() : null;
        if (chatId == null || chatId.isBlank())
            return;

        int globalInterval = global.getIntervalMin();

        List<String> alertLines = new ArrayList<>();

        // controlla segnali numerici
        for (MeasurementSample m : measurements) {
            String key = m.key() != null ? m.key().toLowerCase(Locale.ROOT) : null;
            if (key == null)
                continue;

            AlertConfigSignal cfg = signalRepo
                    .findByProjectIdAndSignalKey(projectId, key).orElse(null);

            Double warning = cfg != null && cfg.getThresholdWarning() != null
                    ? cfg.getThresholdWarning()
                    : defaultWarning(key);
            Double critical = cfg != null && cfg.getThresholdCritical() != null
                    ? cfg.getThresholdCritical()
                    : defaultCritical(key);
            Double warningLow = cfg != null ? cfg.getThresholdWarningLow() : null;
            Double criticalLow = cfg != null ? cfg.getThresholdCriticalLow() : null;

            if (warning == null && critical == null
                    && warningLow == null && criticalLow == null)
                continue;

            // rispetta l'intervallo tra alert
            if (!shouldAlert(cfg, globalInterval))
                continue;

            Double value = m.value();
            if (value == null)
                continue;

            String unit = m.unit() != null ? m.unit() : "";
            String displayName = m.displayName() != null ? m.displayName() : key;
            String line = buildNumericAlertLine(
                    displayName, value, unit, warning, critical, warningLow, criticalLow);

            if (line != null) {
                alertLines.add(line);
                updateLastAlertSent(cfg, projectId, key, globalInterval);
            }
        }

        // controlla booleani e stati
        for (IndicatorSample i : indicators) {
            String key = i.key() != null ? i.key().toLowerCase(Locale.ROOT) : null;
            if (key == null)
                continue;

            AlertConfigSignal cfg = signalRepo
                    .findByProjectIdAndSignalKey(projectId, key).orElse(null);

            Integer triggerValue = cfg != null ? cfg.getTriggerValue() : 1;
            if (triggerValue == null)
                continue;
            if (!shouldAlert(cfg, globalInterval))
                continue;

            Integer value = i.value();
            if (value == null)
                continue;

            String displayName = prettify(key);
            String chartType = SignalDictionary.SIGNALS.containsKey(key)
                    ? SignalDictionary.SIGNALS.get(key).chartType()
                    : "boolean";

            if (chartType.equals("status")) {
                // stato discreto: alert se value >= triggerValue
                if (value >= triggerValue) {
                    String stateName = getStateName(key, value);
                    alertLines.add("🚨 <b>" + displayName + ":</b> " + stateName
                            + " (≥ " + triggerValue + ")");
                    updateLastAlertSent(cfg, projectId, key, globalInterval);
                }
            } else {
                // booleano: alert se value == 1
                if (value == 1) {
                    alertLines.add("🚨 <b>" + displayName + ":</b> Issue");
                    updateLastAlertSent(cfg, projectId, key, globalInterval);
                }
            }
        }

        if (alertLines.isEmpty())
            return;

        // costruisci messaggio unico
        StringBuilder msg = new StringBuilder();
        msg.append("🔴 <b>ALERT — ")
                .append(device.getProject().getName()).append(" / ")
                .append(device.getName()).append("</b>\n");

        if (device.getLatitude() != null && device.getLongitude() != null) {
            msg.append("📍 Lat: ").append(String.format("%.4f", device.getLatitude()))
                    .append(", Lng: ").append(String.format("%.4f", device.getLongitude()))
                    .append("\n");
        }
        msg.append("\n");
        for (String line : alertLines) {
            msg.append(line).append("\n");
        }
        msg.append("\n⏱ Prossimo check tra ").append(globalInterval).append(" min");

        telegramBotService.sendMessage(chatId, msg.toString());
    }

    private String buildNumericAlertLine(String name, double value, String unit,
            Double warning, Double critical,
            Double warningLow, Double criticalLow) {
        String formatted = formatValue(value) + (unit.isBlank() ? "" : " " + unit);

        if (critical != null && value >= critical) {
            return "🔴 <b>" + name + ":</b> " + formatted + " (critical &gt; " + formatValue(critical) + ")";
        }
        if (criticalLow != null && value <= criticalLow) {
            return "🔴 <b>" + name + ":</b> " + formatted + " (critical &lt; " + formatValue(criticalLow) + ")";
        }
        if (warning != null && value >= warning) {
            return "⚠️ <b>" + name + ":</b> " + formatted + " (warning &gt; " + formatValue(warning) + ")";
        }
        if (warningLow != null && value <= warningLow) {
            return "⚠️ <b>" + name + ":</b> " + formatted + " (warning &lt; " + formatValue(warningLow) + ")";
        }
        return null;
    }

    private boolean shouldAlert(AlertConfigSignal cfg, int globalInterval) {
        if (cfg == null || cfg.getLastAlertSentAt() == null)
            return true;
        int interval = (cfg.getIntervalMin() != null)
                ? cfg.getIntervalMin()
                : globalInterval;
        Instant nextAlert = cfg.getLastAlertSentAt()
                .plusSeconds(interval * 60L);
        return Instant.now().isAfter(nextAlert);
    }

    private void updateLastAlertSent(AlertConfigSignal cfg,
            Long projectId, String key,
            int globalInterval) {
        if (cfg == null) {
            cfg = new AlertConfigSignal();
            cfg.setProjectId(projectId);
            cfg.setSignalKey(key);
        }
        cfg.setLastAlertSentAt(Instant.now());
        signalRepo.save(cfg);
    }

    private Double defaultWarning(String key) {
        double[] d = SIGNAL_DEFAULT_THRESHOLDS.get(key);
        return d != null ? d[0] : null;
    }

    private Double defaultCritical(String key) {
        double[] d = SIGNAL_DEFAULT_THRESHOLDS.get(key);
        return d != null ? d[1] : null;
    }

    private String formatValue(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v)
                : String.format("%.1f", v);
    }

    private String prettify(String key) {
        if (key == null)
            return "";
        return java.util.Arrays.stream(key.split("[_\\-]"))
                .map(w -> w.isEmpty() ? w
                        : w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private String getStateName(String key, int value) {
        var cfg = SignalDictionary.SIGNALS.get(key);
        if (cfg != null && cfg.stateLabels() != null) {
            String label = cfg.stateLabels().get(value);
            if (label != null)
                return label;
        }
        return String.valueOf(value);
    }
}