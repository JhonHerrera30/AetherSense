package it.sensorplatform.dto;

import java.util.List;
import java.util.Map;

/**
 * Generic DTO representing a JSON packet sent by a device or an operator.
 * It contains minimal routing information used by the platform.
 */
public class PacketDTO {

    private String macAddress;
    private String devEui;
    private String typeOfDevice;
    private Long projectId;
    private boolean activation;
    private Double latitude;
    private Double longitude;
    private Map<String, Object> payload;
    private List<SpecEntry> spec;
    private List<String> indicator;

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDevEui() {
        return devEui;
    }

    public void setDevEui(String devEui) {
        this.devEui = devEui;
    }

    public String getTypeOfDevice() {
        return typeOfDevice;
    }

    public void setTypeOfDevice(String typeOfDevice) {
        this.typeOfDevice = typeOfDevice;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public boolean isActivation() {
        return activation;
    }

    public void setActivation(boolean activation) {
        this.activation = activation;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public List<SpecEntry> getSpec() {
        return spec;
    }

    public void setSpec(List<SpecEntry> spec) {
        this.spec = spec;
    }

    public List<String> getIndicator() {
        return indicator;
    }

    public void setIndicator(List<String> indicator) {
        this.indicator = indicator;
    }

    public static class SpecEntry {
        private String label;
        private Double min;
        private Double max;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Double getMin() {
            return min;
        }

        public void setMin(Double min) {
            this.min = min;
        }

        public Double getMax() {
            return max;
        }

        public void setMax(Double max) {
            this.max = max;
        }
    }
}

