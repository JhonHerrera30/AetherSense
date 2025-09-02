package it.sensorplatform.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IngestService: punto unico che riceve i dati in stile TTN (deviceId/devEui, timestamp, metrics)
 * e li memorizza in un buffer in-memory per i test locali.
 *
 * Fase 2 (quando vorrai): qui dentro collegheremo i repository per persistere su DB.
 */
@Service
public class IngestService {

    /** Record di un singolo campione ricevuto */
    public static class Sample {
        public final String deviceId;
        public final String devEui;
        public final Instant ts;
        public final Map<String, Object> metrics;

        public Sample(String deviceId, String devEui, Instant ts, Map<String, Object> metrics) {
            this.deviceId = deviceId;
            this.devEui = devEui;
            this.ts = ts;
            this.metrics = metrics != null ? Map.copyOf(metrics) : Map.of();
        }
    }

    /** Ultimi N campioni per device (buffer circolare) */
    private final Map<String, Deque<Sample>> store = new ConcurrentHashMap<>();
    private static final int MAX_SAMPLES_PER_DEVICE = 200;

    /**
     * Normalizza e memorizza un pacchetto ricevuto.
     * @param deviceId nome del device (se null, prova a usare devEui)
     * @param devEui   EUI del device (opzionale, usato come fallback per l'id)
     * @param ts       istante della misura (UTC)
     * @param metrics  mappa chiave=tipo misura, valore=numero (es. temp, hum, pm2p5)
     */
    public void process(String deviceId, String devEui, Instant ts, Map<String, Object> metrics) {
        if (deviceId == null && devEui != null) {
            deviceId = devEui.toLowerCase(Locale.ROOT);
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId o devEui richiesto");
        }
        if (ts == null) ts = Instant.now();

        Deque<Sample> queue = store.computeIfAbsent(deviceId, k -> new ArrayDeque<>());
        queue.addLast(new Sample(deviceId, devEui, ts, metrics));

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
}
