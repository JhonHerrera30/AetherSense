package it.sensorplatform.service;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.dto.UnknownDeviceNotification;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class UnknownDeviceService {
    private final Map<Long, Map<String, UnknownDeviceNotification>> notifications = new ConcurrentHashMap<>();
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void notify(PacketDTO packet) {
        String key = packet.getMacAddress() != null && !packet.getMacAddress().isBlank() ?
                packet.getMacAddress() : packet.getDevEui();
        UnknownDeviceNotification notification = new UnknownDeviceNotification(
                key,
                packet.getMacAddress(),
                packet.getDevEui(),
                packet.getProjectId(),
                packet.getTypeOfDevice(),
                packet.getPayload(),
                Instant.now()
        );
        notifications.computeIfAbsent(packet.getProjectId(), k -> new ConcurrentHashMap<>())
                .put(key, notification);
        List<SseEmitter> list = emitters.getOrDefault(packet.getProjectId(), List.of());
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("unknown-device").data(notification));
            } catch (Exception e) {
                emitter.complete();
            }
        }
    }

    public SseEmitter subscribe(Long projectId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(projectId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Map<String, UnknownDeviceNotification> map = notifications.get(projectId);
        if (map != null && !map.isEmpty()) {
            for (UnknownDeviceNotification notification : map.values()) {
                try {
                    emitter.send(SseEmitter.event().name("unknown-device").data(notification));
                } catch (Exception e) {
                    emitter.complete();
                }
            }
        }

        emitter.onCompletion(() -> emitters.get(projectId).remove(emitter));
        emitter.onTimeout(() -> emitters.get(projectId).remove(emitter));
        return emitter;
    }

    public UnknownDeviceNotification consume(Long projectId, String key) {
        Map<String, UnknownDeviceNotification> map = notifications.get(projectId);
        if (map == null) return null;
        return map.remove(key);
    }
}
