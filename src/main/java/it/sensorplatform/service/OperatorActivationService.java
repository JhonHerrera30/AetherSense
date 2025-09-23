package it.sensorplatform.service;

import it.sensorplatform.dto.OperatorActivationNotification;
import it.sensorplatform.dto.OperatorActivationResolution;
import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Admin;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.repository.DeviceRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class OperatorActivationService {

    private static final String EVENT_NAME = "activation-request";
    private static final String RESPONSE_EVENT_NAME = "activation-response";

    private final Map<Long, Map<Long, OperatorActivationNotification>> notificationsByProject =
            new ConcurrentHashMap<>();
    private final Map<Long, List<SseEmitter>> emittersByOperator = new ConcurrentHashMap<>();

    private final DeviceRepository deviceRepository;

    public OperatorActivationService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

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
            emitToOperator(operatorId, EVENT_NAME, notification);
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

    public Optional<OperatorActivationResolution> respond(Long projectId,
                                                          Long deviceId,
                                                          Credentials operator,
                                                          boolean accepted,
                                                          Double latitude,
                                                          Double longitude) {
        if (projectId == null || deviceId == null || operator == null) {
            return Optional.empty();
        }
        Map<Long, OperatorActivationNotification> map = notificationsByProject.get(projectId);
        if (map == null) {
            return Optional.empty();
        }
        OperatorActivationNotification notification = map.get(deviceId);
        if (notification == null) {
            return Optional.empty();
        }
        Long operatorId = operator.getId();
        if (!notification.isVisibleTo(operatorId)) {
            throw new AccessDeniedException("Operator cannot respond to this activation");
        }

        map.remove(deviceId);
        if (map.isEmpty()) {
            notificationsByProject.remove(projectId);
        }

        Optional<Device> deviceOptional = deviceRepository.findById(deviceId);
        if (deviceOptional.isEmpty()) {
            return Optional.empty();
        }
        Device device = deviceOptional.get();
        if (accepted) {
            if (latitude != null) {
                device.setLatitude(latitude);
            }
            if (longitude != null) {
                device.setLongitude(longitude);
            }
            device.setActivated(true);
            device.setOperator(operator);
        }
        deviceRepository.save(device);

        OperatorActivationResolution resolution = new OperatorActivationResolution(
                notification.getDeviceId(),
                notification.getDeviceName(),
                notification.getMacAddress(),
                notification.getProjectId(),
                notification.getProjectName(),
                accepted,
                latitude,
                longitude,
                operatorId,
                resolveOperatorName(operator),
                device.getTod() != null ? device.getTod().getName() : null,
                Instant.now()
        );

        broadcastResolution(notification, resolution);
        return Optional.of(resolution);
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

    private void emitToOperator(Long operatorId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByOperator.getOrDefault(operatorId, Collections.emptyList());
        for (SseEmitter emitter : new ArrayList<>(emitters)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
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

    private void broadcastResolution(OperatorActivationNotification notification,
                                     OperatorActivationResolution resolution) {
        if (notification == null || resolution == null) {
            return;
        }
        for (Long operatorId : notification.getAuthorizedOperatorIds()) {
            emitToOperator(operatorId, RESPONSE_EVENT_NAME, resolution);
        }
    }

    private String resolveOperatorName(Credentials operator) {
        if (operator == null) {
            return null;
        }
        String visible = operator.getVisibleUsername();
        if (StringUtils.hasText(visible)) {
            return visible;
        }
        return operator.getUsername();
    }
}
