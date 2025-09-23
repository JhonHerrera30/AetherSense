package it.sensorplatform.service;

import it.sensorplatform.dto.OperatorActivationNotification;
import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Admin;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class OperatorActivationService {

    private static final String EVENT_NAME = "activation-request";

    private final Map<Long, Map<Long, OperatorActivationNotification>> notificationsByProject =
            new ConcurrentHashMap<>();
    private final Map<Long, List<SseEmitter>> emittersByOperator = new ConcurrentHashMap<>();

    public void notifyActivation(Device device,
                                 PacketDTO packet,
                                 Admin admin,
                                 List<Credentials> authorizedOperators) {
        if (device == null) {
            return;
        }
        Long deviceId = device.getId();
        if (deviceId == null) {
            return;
        }
        Long projectId = resolveProjectId(device, packet);
        if (projectId == null) {
            return;
        }

        Set<Long> operatorIds = toOperatorIds(authorizedOperators);
        OperatorActivationNotification notification = new OperatorActivationNotification(
                deviceId,
                device.getName(),
                device.getMacAddress(),
                device.getDevEui(),
                projectId,
                device.getProject() != null ? device.getProject().getName() : null,
                admin != null ? admin.getId() : null,
                resolveAdminEmail(admin, device),
                resolveLatitude(device, packet),
                resolveLongitude(device, packet),
                Instant.now(),
                operatorIds
        );

        notificationsByProject
                .computeIfAbsent(projectId, id -> new ConcurrentHashMap<>())
                .put(deviceId, notification);

        if (operatorIds.isEmpty()) {
            return;
        }

        for (Long operatorId : operatorIds) {
            emitToOperator(operatorId, notification);
        }
    }

    public List<OperatorActivationNotification> listForOperator(Long projectId, Long operatorId) {
        if (projectId == null || operatorId == null) {
            return List.of();
        }
        Map<Long, OperatorActivationNotification> map = notificationsByProject.get(projectId);
        if (map == null || map.isEmpty()) {
            return List.of();
        }
        return map.values().stream()
                .filter(notification -> notification.isVisibleTo(operatorId))
                .sorted(java.util.Comparator.comparing(OperatorActivationNotification::getTimestamp))
                .toList();
    }

    public OperatorActivationNotification consume(Long projectId, Long deviceId, Long operatorId) {
        if (projectId == null || deviceId == null || operatorId == null) {
            return null;
        }
        Map<Long, OperatorActivationNotification> map = notificationsByProject.get(projectId);
        if (map == null) {
            return null;
        }
        OperatorActivationNotification notification = map.get(deviceId);
        if (notification == null || !notification.isVisibleTo(operatorId)) {
            return null;
        }
        map.remove(deviceId);
        if (map.isEmpty()) {
            notificationsByProject.remove(projectId);
        }
        return notification;
    }

    public SseEmitter subscribe(Long operatorId, Long projectId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emittersByOperator
                .computeIfAbsent(operatorId, id -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(operatorId, emitter));
        emitter.onTimeout(() -> removeEmitter(operatorId, emitter));

        replayExisting(operatorId, projectId, emitter);
        return emitter;
    }

    private void emitToOperator(Long operatorId, OperatorActivationNotification notification) {
        List<SseEmitter> emitters = emittersByOperator.getOrDefault(operatorId, Collections.emptyList());
        for (SseEmitter emitter : new ArrayList<>(emitters)) {
            try {
                emitter.send(SseEmitter.event().name(EVENT_NAME).data(notification));
            } catch (Exception e) {
                emitter.complete();
                removeEmitter(operatorId, emitter);
            }
        }
    }

    private void replayExisting(Long operatorId, Long projectId, SseEmitter emitter) {
        if (operatorId == null || projectId == null) {
            return;
        }
        Map<Long, OperatorActivationNotification> map = notificationsByProject.get(projectId);
        if (map == null || map.isEmpty()) {
            return;
        }
        for (OperatorActivationNotification notification : map.values()) {
            if (!notification.isVisibleTo(operatorId)) {
                continue;
            }
            try {
                emitter.send(SseEmitter.event().name(EVENT_NAME).data(notification));
            } catch (Exception e) {
                emitter.complete();
                removeEmitter(operatorId, emitter);
                break;
            }
        }
    }

    private void removeEmitter(Long operatorId, SseEmitter emitter) {
        if (operatorId == null || emitter == null) {
            return;
        }
        List<SseEmitter> emitters = emittersByOperator.get(operatorId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByOperator.remove(operatorId);
        }
    }

    private Long resolveProjectId(Device device, PacketDTO packet) {
        if (device.getProject() != null) {
            return device.getProject().getId();
        }
        return packet != null ? packet.getProjectId() : null;
    }

    private String resolveAdminEmail(Admin admin, Device device) {
        if (admin != null && admin.getCredentials() != null
                && StringUtils.hasText(admin.getCredentials().getEmail())) {
            return admin.getCredentials().getEmail();
        }
        return device.getEmailOwner();
    }

    private Double resolveLatitude(Device device, PacketDTO packet) {
        if (packet != null && packet.getLatitude() != null) {
            return packet.getLatitude();
        }
        return device.getLatitude();
    }

    private Double resolveLongitude(Device device, PacketDTO packet) {
        if (packet != null && packet.getLongitude() != null) {
            return packet.getLongitude();
        }
        return device.getLongitude();
    }

    private Set<Long> toOperatorIds(List<Credentials> operators) {
        if (operators == null || operators.isEmpty()) {
            return Set.of();
        }
        return operators.stream()
                .filter(Objects::nonNull)
                .map(Credentials::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
