package it.sensorplatform.controller.rest;

import static it.sensorplatform.model.Credentials.FIRE_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.LTRAD_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.VOLCANO_ADMIN_ROLE;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

import it.sensorplatform.dto.AggregatedPointDTO;
import it.sensorplatform.dto.SpecDTO;
import it.sensorplatform.dto.TelemetrySampleDTO;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Project;
import it.sensorplatform.model.SampleEntity;
import it.sensorplatform.model.MeasurementEntity;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.DeviceService;
import it.sensorplatform.service.IngestService;
import it.sensorplatform.util.MacAddressUtils;
import it.sensorplatform.repository.SampleRepository;
import it.sensorplatform.dto.AggregatedPointDTO;

@RestController
@RequestMapping("/api/admin/devices")
public class DeviceTelemetryController {

    private static final int MAX_HISTORY = 200;

    private final DeviceService deviceService;
    private final CredentialsService credentialsService;
    private final IngestService ingestService;
    private final SampleRepository sampleRepository;

    public DeviceTelemetryController(DeviceService deviceService,
            CredentialsService credentialsService,
            IngestService ingestService,
            SampleRepository sampleRepository) {
        this.deviceService = deviceService;
        this.credentialsService = credentialsService;
        this.ingestService = ingestService;
        this.sampleRepository = sampleRepository;
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
        List<SampleEntity> samples = sampleRepository.findByDeviceIdOrderByTimestamp(telemetryKey);
        if (samples.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        SampleEntity sample = samples.get(samples.size() - 1);
        List<SpecDTO> specs = sample.getMeasurements().stream()
                .filter(m -> m.getType() == MeasurementEntity.MeasurementType.MEASUREMENT)
                .map(m -> {
                    it.sensorplatform.util.SignalDictionary.ChartConfig config = it.sensorplatform.util.SignalDictionary
                            .getConfig(
                                    m.getKey(), m.getMin(), m.getMax());
                    return new SpecDTO(
                            m.getKey(),
                            config.displayName(),
                            config.displayName(),
                            m.getComponent(),
                            config.unit(),
                            config.defaultMin(),
                            config.defaultMax());
                })
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

        List<SampleEntity> all = sampleRepository.findByDeviceIdOrderByTimestamp(telemetryKey);
        if (all.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        TelemetrySampleDTO dto = TelemetrySampleDTO.fromEntity(all.get(all.size() - 1));
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
        List<SampleEntity> all = sampleRepository.findByDeviceIdOrderByTimestamp(telemetryKey);
        List<SampleEntity> limited = all.subList(Math.max(0, all.size() - safeLimit), all.size());
        List<TelemetrySampleDTO> history = limited.stream()
                .map(TelemetrySampleDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{macAddress}/telemetry/aggregated")
    public ResponseEntity<List<AggregatedPointDTO>> getAggregated(
            @PathVariable("macAddress") String deviceKey,
            @RequestParam(value = "period", defaultValue = "day") String period,
            @RequestParam(value = "days", defaultValue = "7") int days) {

        Optional<Device> deviceOpt = resolveAuthorizedDevice(deviceKey);
        if (deviceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String telemetryKey = telemetryIdentifier(deviceOpt.get());
        if (telemetryKey == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // whitelist period — previene SQL injection sul DATE_TRUNC
        String safePeriod = switch (period) {
            case "hour", "day", "week" -> period;
            default -> "day";
        };

        int safeDays = Math.max(1, Math.min(days, 365));

        Instant to = Instant.now();
        Instant from = to.minus(safeDays, ChronoUnit.DAYS);

        List<Object[]> rows = sampleRepository.findAggregated(
                telemetryKey, from, to, safePeriod);

        List<AggregatedPointDTO> result = rows.stream()
                .map(AggregatedPointDTO::fromRow)
                .toList();

        return ResponseEntity.ok(result);
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
