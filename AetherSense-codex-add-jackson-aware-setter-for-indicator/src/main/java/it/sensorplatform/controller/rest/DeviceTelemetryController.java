package it.sensorplatform.controller.rest;

import static it.sensorplatform.model.Credentials.FIRE_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.LTRAD_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.VOLCANO_ADMIN_ROLE;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.sensorplatform.dto.SpecDTO;
import it.sensorplatform.dto.TelemetrySampleDTO;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Project;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.DeviceService;
import it.sensorplatform.service.IngestService;
import it.sensorplatform.util.MacAddressUtils;

@RestController
@RequestMapping("/api/admin/devices")
public class DeviceTelemetryController {

    private static final int MAX_HISTORY = 200;

    private final DeviceService deviceService;
    private final CredentialsService credentialsService;
    private final IngestService ingestService;

    public DeviceTelemetryController(DeviceService deviceService,
                                     CredentialsService credentialsService,
                                     IngestService ingestService) {
        this.deviceService = deviceService;
        this.credentialsService = credentialsService;
        this.ingestService = ingestService;
    }

    @GetMapping("/{macAddress}/specs")
    public ResponseEntity<List<SpecDTO>> getDeviceSpecs(@PathVariable("macAddress") String deviceKey) {
        Optional<Device> deviceOpt = resolveAuthorizedDevice(deviceKey);
        if (deviceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String telemetryKey = telemetryIdentifier(deviceOpt.get());
        if (telemetryKey == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<IngestService.Sample> samples = ingestService.last(telemetryKey, 1);
        if (samples.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        IngestService.Sample sample = samples.get(samples.size() - 1);
        List<SpecDTO> specs = sample.measurements().stream()
                .map(SpecDTO::fromMeasurement)
                .collect(Collectors.toList());
        return ResponseEntity.ok(specs);
    }

    @GetMapping("/{macAddress}/telemetry/latest")
    public ResponseEntity<TelemetrySampleDTO> getLatestSample(@PathVariable("macAddress") String deviceKey) {
        Optional<Device> deviceOpt = resolveAuthorizedDevice(deviceKey);
        if (deviceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String telemetryKey = telemetryIdentifier(deviceOpt.get());
        if (telemetryKey == null) {
            return ResponseEntity.noContent().build();
        }
        List<IngestService.Sample> samples = ingestService.last(telemetryKey, 1);
        if (samples.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        TelemetrySampleDTO dto = TelemetrySampleDTO.fromSample(samples.get(samples.size() - 1));
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{macAddress}/telemetry/history")
    public ResponseEntity<List<TelemetrySampleDTO>> getHistory(@PathVariable("macAddress") String deviceKey,
                                                               @RequestParam(value = "limit", defaultValue = "50") int limit) {
        Optional<Device> deviceOpt = resolveAuthorizedDevice(deviceKey);
        if (deviceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String telemetryKey = telemetryIdentifier(deviceOpt.get());
        if (telemetryKey == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        int safeLimit = Math.max(1, Math.min(limit, MAX_HISTORY));
        List<TelemetrySampleDTO> history = ingestService.last(telemetryKey, safeLimit).stream()
                .map(TelemetrySampleDTO::fromSample)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    private Optional<Device> resolveAuthorizedDevice(String deviceKey) {
        Credentials credentials = currentCredentials();
        if (credentials == null || credentials.getAdmin() == null) {
            return Optional.empty();
        }

        Optional<Device> deviceOpt = deviceService.findOptionalByDeviceKey(deviceKey);
        if (deviceOpt.isEmpty()) {
            return Optional.empty();
        }

        Device device = deviceOpt.get();
        if (!isAuthorized(credentials, device)) {
            return Optional.empty();
        }
        return Optional.of(device);
    }

    private String telemetryIdentifier(Device device) {
        String mac = MacAddressUtils.normalize(device.getMacAddress());
        if (mac != null && !mac.isBlank()) {
            return mac;
        }
        String devEui = device.getDevEui();
        if (devEui == null || devEui.isBlank()) {
            return null;
        }
        return devEui.toLowerCase(Locale.ROOT);
    }

    private Credentials currentCredentials() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return null;
        }
        return credentialsService.getCredentials(userDetails.getUsername());
    }

    private boolean isAuthorized(Credentials credentials, Device device) {
        Project project = device.getProject();
        if (project == null || project.getId() == null) {
            return false;
        }
        if (!Objects.equals(project.getId(), credentials.getProjectId())) {
            return false;
        }
        String role = credentials.getRole();
        return LTRAD_ADMIN_ROLE.equals(role)
                || FIRE_ADMIN_ROLE.equals(role)
                || VOLCANO_ADMIN_ROLE.equals(role);
    }
}

