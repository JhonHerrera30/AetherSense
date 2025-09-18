package it.sensorplatform.controller.rest;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.dto.UnknownDeviceNotification;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Indicator;
import it.sensorplatform.model.Project;
import it.sensorplatform.model.Spec;
import it.sensorplatform.model.TypeOfDevice;
import it.sensorplatform.repository.DeviceRepository;
import it.sensorplatform.repository.ProjectRepository;
import it.sensorplatform.repository.TypeOfDeviceRepository;
import it.sensorplatform.service.IndicatorService;
import it.sensorplatform.service.SpecService;
import it.sensorplatform.service.UnknownDeviceService;
import it.sensorplatform.util.MacAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/notifications")
public class NotificationControllerRest {
    private final UnknownDeviceService unknownDeviceService;
    private final DeviceRepository deviceRepository;
    private final TypeOfDeviceRepository typeOfDeviceRepository;
    private final SpecService specService;
    private final IndicatorService indicatorService;
    private final ProjectRepository projectRepository;
    private static final Logger logger = LoggerFactory.getLogger(NotificationControllerRest.class);

    public NotificationControllerRest(UnknownDeviceService unknownDeviceService,
                                      DeviceRepository deviceRepository,
                                      TypeOfDeviceRepository typeOfDeviceRepository,
                                      SpecService specService,
                                      IndicatorService indicatorService,
                                      ProjectRepository projectRepository) {
        this.unknownDeviceService = unknownDeviceService;
        this.deviceRepository = deviceRepository;
        this.typeOfDeviceRepository = typeOfDeviceRepository;
        this.specService = specService;
        this.indicatorService = indicatorService;
        this.projectRepository = projectRepository;
    }

    @GetMapping("/stream/{projectId}")
    public SseEmitter stream(@PathVariable Long projectId) {
        return unknownDeviceService.subscribe(projectId);
    }

    @PostMapping("/{projectId}/{key}/add")
    public ResponseEntity<Void> addDevice(@PathVariable Long projectId, @PathVariable String key) {
        String normalizedKey = MacAddressUtils.normalize(key);
        UnknownDeviceNotification notif = unknownDeviceService.consume(projectId, normalizedKey);
        if (notif == null) {
            logger.warn("No notification found for project {} and key {}", projectId, normalizedKey);
            return ResponseEntity.notFound().build();
        }
        logger.info("Consumed notification for project {} key {}: mac {} devEui {}", projectId, normalizedKey, notif.getMacAddress(), notif.getDevEui());
        Project project = projectRepository.findById(projectId).orElse(null);
        TypeOfDevice tod = typeOfDeviceRepository.findByName(notif.getTypeOfDevice()).orElseGet(() -> {
            TypeOfDevice created = new TypeOfDevice();
            created.setName(notif.getTypeOfDevice());
            return created;
        });

        boolean specsUpdated = ensureSpecs(tod, notif.getSpec());
        boolean indicatorsUpdated = ensureIndicators(tod, notif.getIndicator());
        if (tod.getId() == null || specsUpdated || indicatorsUpdated) {
            typeOfDeviceRepository.save(tod);
        }

        String macAddress = MacAddressUtils.normalize(notif.getMacAddress());
        String normalizedDevEui = MacAddressUtils.normalize(notif.getDevEui());

        boolean macConflict = macAddress != null && !macAddress.isBlank() && deviceRepository.existsByMacAddress(macAddress);
        boolean devEuiConflict = normalizedDevEui != null && !normalizedDevEui.isBlank()
                && deviceRepository.existsByDevEui(normalizedDevEui);
        if (macConflict || devEuiConflict) {
            logger.warn("Device with MAC {} or DevEUI {} already exists", macAddress, normalizedDevEui);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (macAddress == null || macAddress.isBlank()) {
            logger.info("MAC address missing; persisting device with DevEUI {} only", normalizedDevEui);
        }

        logger.info("Creating device with MAC {} and DevEUI {}", macAddress, normalizedDevEui);
        Device device = new Device();
        device.setMacAddress(macAddress);
        device.setDevEui(normalizedDevEui);
        device.setProject(project);
        device.setTod(tod);
        device.setStatus("deactivated");
        try {
            Device savedDevice = deviceRepository.save(device);
            logger.info("Persisted device with id {} and status {}", savedDevice.getId(), savedDevice.getStatus());
            return ResponseEntity.ok().build();
        } catch (DataIntegrityViolationException e) {
            logger.warn("Conflict saving device with MAC {} and DevEUI {}", macAddress, notif.getDevEui(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Error saving device with MAC {} and DevEUI {}", macAddress, notif.getDevEui(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean ensureSpecs(TypeOfDevice tod, List<PacketDTO.SpecEntry> specEntries) {
        List<Spec> specs = tod.getSpecs() != null ? new ArrayList<>(tod.getSpecs()) : new ArrayList<>();
        boolean updated = false;
        if (specEntries != null) {
            for (PacketDTO.SpecEntry specEntry : specEntries) {
                if (specEntry == null) {
                    continue;
                }
                String label = specEntry.getLabel();
                String specKey = sanitize(specEntry.getKey());
                if (label == null && specKey == null) {
                    continue;
                }
                Spec spec = new Spec();
                String component;
                String unitOfMeasurement;
                if (label != null) {
                    String[] parts = label.split("-");
                    component = parts.length > 0 ? sanitize(parts[0]) : "";
                    unitOfMeasurement = parts.length > 2 ? sanitize(parts[2]) : "";
                } else {
                    component = "";
                    unitOfMeasurement = "";
                }
                String measurement = determineMeasurement(label, specKey);
                final String normalizedComponent = component != null ? component : "";
                final String normalizedUnitOfMeasurement = unitOfMeasurement != null ? unitOfMeasurement : "";
                final String normalizedMeasurement = measurement != null ? measurement : "";

                List<String> legacyCandidates = new ArrayList<>();
                String sanitizedLabel = label != null ? sanitize(label) : null;
                if (sanitizedLabel != null) {
                    legacyCandidates.add(sanitizedLabel);
                }
                if (specKey != null) {
                    legacyCandidates.add(specKey);
                }

                if (!legacyCandidates.isEmpty()) {
                    Spec legacySpec = specs.stream()
                            .filter(existing -> normalizedComponent.equals(existing.getComponent())
                                    && legacyCandidates.contains(existing.getMeasurement()))
                            .findFirst()
                            .orElse(null);
                    if (legacySpec != null) {
                        boolean changed = false;
                        if (!normalizedMeasurement.equals(legacySpec.getMeasurement())) {
                            legacySpec.setMeasurement(normalizedMeasurement);
                            changed = true;
                        }
                        if (!normalizedUnitOfMeasurement.equals(legacySpec.getUnitOfMeasurement())) {
                            legacySpec.setUnitOfMeasurement(normalizedUnitOfMeasurement);
                            changed = true;
                        }
                        if (changed) {
                            Spec savedLegacy = specService.save(legacySpec);
                            int index = specs.indexOf(legacySpec);
                            if (index >= 0) {
                                specs.set(index, savedLegacy);
                            }
                            updated = true;
                        }
                        continue;
                    }
                }

                spec.setComponent(normalizedComponent);
                spec.setUnitOfMeasurement(normalizedUnitOfMeasurement);
                spec.setMeasurement(normalizedMeasurement);
                Spec managedSpec = specService.findByFields(spec)
                        .orElseGet(() -> specService.save(spec));
                if (!specs.contains(managedSpec)) {
                    specs.add(managedSpec);
                    updated = true;
                }
            }
        }
        if (tod.getSpecs() == null || updated) {
            tod.setSpecs(specs);
        }
        return updated;
    }

    private String determineMeasurement(String label, String specKey) {
        String measurementFromLabel = extractMeasurementSegment(label);
        if (measurementFromLabel != null) {
            return measurementFromLabel;
        }
        String measurementFromKey = extractMeasurementSegment(specKey);
        if (measurementFromKey != null) {
            return measurementFromKey;
        }
        return sanitize(specKey);
    }

    private String extractMeasurementSegment(String value) {
        String sanitized = sanitize(value);
        if (sanitized == null) {
            return null;
        }
        String[] parts = sanitized.split("-");
        if (parts.length < 2) {
            return null;
        }
        if (parts.length == 2) {
            return sanitize(parts[1]);
        }
        for (int i = 1; i < parts.length - 1; i++) {
            String candidate = sanitize(parts[i]);
            if (candidate != null) {
                return candidate;
            }
        }
        return sanitize(parts[1]);
    }

    private boolean ensureIndicators(TypeOfDevice tod, List<String> indicatorEntries) {
        List<Indicator> indicators = tod.getIndicators() != null ? new ArrayList<>(tod.getIndicators()) : new ArrayList<>();
        boolean updated = false;
        if (indicatorEntries != null) {
            for (String raw : indicatorEntries) {
                Indicator parsed = parseIndicator(raw);
                if (parsed == null) {
                    continue;
                }
                Indicator managed = indicatorService.findByKey(parsed.getKey())
                        .map(existing -> updateIndicatorName(existing, parsed.getName()))
                        .orElseGet(() -> indicatorService.save(parsed));
                if (!indicators.contains(managed)) {
                    indicators.add(managed);
                    updated = true;
                }
            }
        }
        if (tod.getIndicators() == null || updated) {
            tod.setIndicators(indicators);
        }
        return updated;
    }

    private Indicator parseIndicator(String raw) {
        String sanitized = sanitize(raw);
        if (sanitized == null) {
            return null;
        }
        int separator = findSeparator(sanitized);
        String keyPart;
        String labelPart;
        if (separator > 0) {
            keyPart = sanitize(sanitized.substring(0, separator));
            labelPart = sanitize(sanitized.substring(separator + 1));
        } else {
            keyPart = null;
            labelPart = sanitized;
        }
        String key = keyPart != null ? keyPart : deriveKeyFromLabel(labelPart);
        if (key == null) {
            return null;
        }
        String label = labelPart != null ? labelPart : prettify(key);
        Indicator indicator = new Indicator();
        indicator.setKey(key);
        indicator.setName(label != null ? label : key);
        return indicator;
    }

    private Indicator updateIndicatorName(Indicator indicator, String candidate) {
        String sanitizedCandidate = sanitize(candidate);
        if (sanitizedCandidate != null && !sanitizedCandidate.equals(indicator.getName())) {
            indicator.setName(sanitizedCandidate);
            return indicatorService.save(indicator);
        }
        return indicator;
    }

    private int findSeparator(String value) {
        int colon = value.indexOf(':');
        int equal = value.indexOf('=');
        int pipe = value.indexOf('|');
        int separator = colon > 0 ? colon : -1;
        if (separator < 0 || (equal > 0 && equal < separator)) {
            separator = equal;
        }
        if (separator < 0 || (pipe > 0 && pipe < separator)) {
            separator = pipe;
        }
        return separator;
    }

    private String deriveKeyFromLabel(String label) {
        String sanitized = sanitize(label);
        if (sanitized == null) {
            return null;
        }
        return sanitized.replace(' ', '_').toLowerCase(Locale.ROOT);
    }

    private String prettify(String value) {
        String sanitized = sanitize(value);
        if (sanitized == null) {
            return null;
        }
        String replaced = sanitized.replace('_', ' ').replace('-', ' ').trim();
        if (replaced.isEmpty()) {
            return sanitized;
        }
        String[] parts = replaced.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase());
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase());
            }
        }
        return builder.length() > 0 ? builder.toString() : sanitized;
    }
}
