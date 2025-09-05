package it.sensorplatform.service;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Project;
import it.sensorplatform.repository.DeviceRepository;
import it.sensorplatform.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Service that applies the device/project filter logic described in the
 * specification. It handles three cases:
 *  1. unknown device -> register it with activation=false
 *  2. known device with activation flag -> update device activation and location
 *  3. known device without activation flag -> forward data to IngestService
 */
@Service
public class PacketService {

    private final DeviceRepository deviceRepository;
    private final ProjectRepository projectRepository;
    private final IngestService ingestService;

    public PacketService(DeviceRepository deviceRepository,
                         ProjectRepository projectRepository,
                         IngestService ingestService) {
        this.deviceRepository = deviceRepository;
        this.projectRepository = projectRepository;
        this.ingestService = ingestService;
    }

    public enum Result { NEW_DEVICE, ACTIVATION, DATA }

    /**
     * Process an incoming packet according to device/project state.
     */
    public Result handlePacket(PacketDTO packet) {
        if (packet.getMacAddress() == null || packet.getMacAddress().isBlank()) {
            throw new IllegalArgumentException("macAddress is required");
        }

        Optional<Device> existing = deviceRepository.findByMacAddress(packet.getMacAddress());
        if (existing.isEmpty()) {
            // Case 1: device not present -> register
            Device device = new Device();
            device.setMacAddress(packet.getMacAddress());
            device.setName(packet.getMacAddress());
            device.setEmailOwner("");
            device.setStatus("deactivated");
            device.setLatitude(packet.getLatitude() != null ? packet.getLatitude() : 0d);
            device.setLongitude(packet.getLongitude() != null ? packet.getLongitude() : 0d);
            if (packet.getProjectId() != null) {
                Project p = projectRepository.findById(packet.getProjectId()).orElse(null);
                device.setProject(p);
            }
            deviceRepository.save(device);
            return Result.NEW_DEVICE;
        }

        Device device = existing.get();

        if (packet.isActivation()) {
            // Case 3: activation packet -> update flags and location
            device.setStatus("activated");
            if (packet.getLatitude() != null) device.setLatitude(packet.getLatitude());
            if (packet.getLongitude() != null) device.setLongitude(packet.getLongitude());
            deviceRepository.save(device);
            return Result.ACTIVATION;
        }

        // Case 2: normal data packet -> forward metrics to ingest service
        Map<String, Object> payload = packet.getPayload();
        ingestService.process(device.getMacAddress(), device.getDevEui(), Instant.now(), payload);
        return Result.DATA;
    }
}

