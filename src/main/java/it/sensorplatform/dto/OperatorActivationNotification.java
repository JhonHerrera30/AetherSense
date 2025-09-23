package it.sensorplatform.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Notification emitted when a device that is not yet activated sends
 * a packet and requires an operator intervention.
 */
public class OperatorActivationNotification {

    private final Long deviceId;
    private final String deviceName;
    private final String macAddress;
    private final String devEui;
    private final Long projectId;
    private final String projectName;
    private final Long adminId;
    private final String adminEmail;
    private final Double latitude;
    private final Double longitude;
    private final Instant timestamp;
    private final Set<Long> authorizedOperatorIds;

    public OperatorActivationNotification(Long deviceId,
                                          String deviceName,
                                          String macAddress,
                                          String devEui,
                                          Long projectId,
                                          String projectName,
                                          Long adminId,
                                          String adminEmail,
                                          Double latitude,
                                          Double longitude,
                                          Instant timestamp,
                                          Set<Long> authorizedOperatorIds) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.devEui = devEui;
        this.projectId = projectId;
        this.projectName = projectName;
        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.authorizedOperatorIds = authorizedOperatorIds != null
                ? Collections.unmodifiableSet(authorizedOperatorIds)
                : Set.of();
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getDevEui() {
        return devEui;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public Long getAdminId() {
        return adminId;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @JsonIgnore
    public Set<Long> getAuthorizedOperatorIds() {
        return authorizedOperatorIds;
    }

    public boolean isVisibleTo(Long operatorId) {
        if (operatorId == null) {
            return false;
        }
        return authorizedOperatorIds.contains(operatorId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperatorActivationNotification that = (OperatorActivationNotification) o;
        return Objects.equals(deviceId, that.deviceId)
                && Objects.equals(projectId, that.projectId)
                && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, projectId, timestamp);
    }
}
