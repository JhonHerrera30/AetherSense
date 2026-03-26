package it.sensorplatform.dto;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;

/**
 * Generic DTO representing a JSON packet sent by a device or an operator.
 * It contains minimal routing information used by the platform.
 */
public class PacketDTO {

    private String macAddress;
    private String devEui;
    private String typeOfDevice;
    private Long projectId;
    private Double latitude;
    private Double longitude;
    private Map<String, Object> payload;
    private List<SpecEntry> spec;
    private List<String> indicator;
    private Instant timestamp;

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

    public Instant getTimestamp(){
        return timestamp;
    }
    public void setTimestamp(Instant timestamp){
        this.timestamp = timestamp;
    }

    @JsonSetter("indicator")
    public void setIndicator(List<?> indicator) {
        if (indicator == null) {
            this.indicator = null;
            return;
        }
        List<String> normalized = new ArrayList<>(indicator.size());
        for (Object entry : indicator) {
            normalized.add(normalizeIndicatorEntry(entry));
        }
        this.indicator = normalized;
    }

    private String normalizeIndicatorEntry(Object entry) {
        if (entry == null) {
            return null;
        }
        if (entry instanceof String stringEntry) {
            return stringEntry;
        }
        if (entry instanceof Map<?, ?> mapEntry) {
            String key = extractFromMap(mapEntry, "key");
            String label = extractFromMap(mapEntry, "label", "name");
            return buildCanonicalIndicator(key, label, mapEntry);
        }
        String key = extractProperty(entry, "getKey", "key");
        String label = extractProperty(entry, "getLabel", "getName", "label", "name");
        return buildCanonicalIndicator(key, label, entry);
    }

    private String extractFromMap(Map<?, ?> map, String... keys) {
        for (String candidate : keys) {
            Object value = map.get(candidate);
            if (value == null) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && candidate.equalsIgnoreCase(key)) {
                        value = entry.getValue();
                        break;
                    }
                }
            }
            String extracted = trimToNull(asString(value));
            if (extracted != null) {
                return extracted;
            }
        }
        return null;
    }

    private String extractProperty(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Object value = target.getClass().getMethod(methodName).invoke(target);
                String extracted = trimToNull(asString(value));
                if (extracted != null) {
                    return extracted;
                }
            } catch (NoSuchMethodException ignored) {
                // Accessor not exposed, continue with the next candidate
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                // Accessor not accessible, continue with the next candidate
            }
        }
        return null;
    }

    private String buildCanonicalIndicator(String key, String label, Object fallback) {
        String normalizedKey = trimToNull(key);
        String normalizedLabel = trimToNull(label);
        if (normalizedKey != null && normalizedLabel != null) {
            return normalizedKey + ":" + normalizedLabel;
        }
        if (normalizedKey != null) {
            return normalizedKey;
        }
        if (normalizedLabel != null) {
            return normalizedLabel;
        }
        return fallback != null ? fallback.toString() : null;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return value.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class SpecEntry {
        private String key;
        private String label;
        private Double min;
        private Double max;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

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

