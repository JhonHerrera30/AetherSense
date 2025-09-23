package it.sensorplatform.dto;

import java.time.Instant;

/**
 * DTO emitted when an operator responds to an activation request.
 */
public class OperatorActivationResolution {

    private final Long deviceId;
    private final String deviceName;
    private final String macAddress;
    private final Long projectId;
    private final String projectName;
    private final boolean accepted;
    private final Double latitude;
    private final Double longitude;
    private final Long operatorId;
    private final String operatorName;
    private final String deviceType;
    private final Instant timestamp;

    public OperatorActivationResolution(Long deviceId,
                                        String deviceName,
                                        String macAddress,
                                        Long projectId,
                                        String projectName,
                                        boolean accepted,
                                        Double latitude,
                                        Double longitude,
                                        Long operatorId,
                                        String operatorName,
                                        String deviceType,
                                        Instant timestamp) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.projectId = projectId;
        this.projectName = projectName;
        this.accepted = accepted;
        this.latitude = latitude;
        this.longitude = longitude;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.deviceType = deviceType;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
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

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
