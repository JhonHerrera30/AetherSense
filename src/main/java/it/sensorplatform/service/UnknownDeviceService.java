package it.sensorplatform.service;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.dto.UnknownDeviceNotification;
import it.sensorplatform.util.MacAddressUtils;
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
        String mac = MacAddressUtils.normalize(packet.getMacAddress());
        String devEui = MacAddressUtils.normalize(packet.getDevEui());
        String key = mac != null && !mac.isBlank() ? mac : devEui;
        key = MacAddressUtils.normalize(key);
        System.out.println("UnknownDeviceService.notify - unknown device key: " + key + ", project: " + packet.getProjectId());
        UnknownDeviceNotification notification = new UnknownDeviceNotification(
                key,
                mac,
                devEui,
                packet.getProjectId(),
                packet.getTypeOfDevice(),
                packet.getPayload(),
                Instant.now()
        );
        notifications.computeIfAbsent(packet.getProjectId(), k -> new ConcurrentHashMap<>())
                .put(key, notification);
        List<SseEmitter> list = emitters.getOrDefault(packet.getProjectId(), List.of());
        System.out.println("UnknownDeviceService.notify - sending notification to " + list.size() + " emitter(s)");
        for (SseEmitter emitter : list) {
            try {
                System.out.println("UnknownDeviceService.notify - sending SSE event");
                emitter.send(SseEmitter.event().name("unknown-device").data(notification));
            } catch (Exception e) {
                System.out.println("UnknownDeviceService.notify - emitter failed, completing");
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
        key = MacAddressUtils.normalize(key);
        Map<String, UnknownDeviceNotification> map = notifications.get(projectId);
        if (map == null) return null;
        return map.remove(key);
    }
}
