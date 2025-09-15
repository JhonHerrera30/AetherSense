package it.sensorplatform.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class UnknownDeviceNotification {
    private String key;
    private String macAddress;
    private String devEui;
    private Long projectId;
    private String typeOfDevice;
    private Map<String, Object> payload;
    private List<String> spec;
    private Instant timestamp;

    public UnknownDeviceNotification(String key,
                                     String macAddress,
                                     String devEui,
                                     Long projectId,
                                     String typeOfDevice,
                                     Map<String, Object> payload,
                                     List<String> spec,
                                     Instant timestamp) {
        this.key = key;
        this.macAddress = macAddress;
        this.devEui = devEui;
        this.projectId = projectId;
        this.typeOfDevice = typeOfDevice;
        this.payload = payload;
        this.spec = spec;
        this.timestamp = timestamp;
    }

    public String getKey() { return key; }
    public String getMacAddress() { return macAddress; }
    public String getDevEui() { return devEui; }
    public Long getProjectId() { return projectId; }
    public String getTypeOfDevice() { return typeOfDevice; }
    public Map<String, Object> getPayload() { return payload; }
    public List<String> getSpec() { return spec; }
    public Instant getTimestamp() { return timestamp; }
}
