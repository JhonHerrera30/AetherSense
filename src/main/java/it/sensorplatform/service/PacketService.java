package it.sensorplatform.service;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Device;
import it.sensorplatform.repository.DeviceRepository;
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
    private final IngestService ingestService;
    private final UnknownDeviceService unknownDeviceService;

    public PacketService(DeviceRepository deviceRepository,
                         IngestService ingestService,
                         UnknownDeviceService unknownDeviceService) {
        this.deviceRepository = deviceRepository;
        this.ingestService = ingestService;
        this.unknownDeviceService = unknownDeviceService;
    }

    public enum Result { NEW_DEVICE, ACTIVATION, DATA }

    /**
     * Process an incoming packet according to device/project state.
     */
    public Result handlePacket(PacketDTO packet) {
        System.out.println("PacketService.handlePacket - processing packet: " + packet);
        if ((packet.getMacAddress() == null || packet.getMacAddress().isBlank()) &&
                (packet.getDevEui() == null || packet.getDevEui().isBlank())) {
            throw new IllegalArgumentException("macAddress or devEui is required");
        }

        Optional<Device> existing = Optional.empty();
        if (packet.getMacAddress() != null && !packet.getMacAddress().isBlank()) {
            existing = deviceRepository.findByMacAddress(packet.getMacAddress());
        }
        if (existing.isEmpty() && packet.getDevEui() != null && !packet.getDevEui().isBlank()) {
            existing = deviceRepository.findByDevEui(packet.getDevEui());
        }
        if (existing.isEmpty()) {
            System.out.println("PacketService.handlePacket - device not found, notifying UnknownDeviceService");
            unknownDeviceService.notify(packet);
            return Result.NEW_DEVICE;
        }

        Device device = existing.get();

        if (packet.isActivation()) {
            System.out.println("PacketService.handlePacket - activation packet for device: " + device.getId());
            // Case 3: activation packet -> update flags and location
            device.setStatus("activated");
            if (packet.getLatitude() != null) device.setLatitude(packet.getLatitude());
            if (packet.getLongitude() != null) device.setLongitude(packet.getLongitude());
            deviceRepository.save(device);
            return Result.ACTIVATION;
        }

        System.out.println("PacketService.handlePacket - forwarding data to IngestService for device: " + device.getMacAddress());
        // Case 2: normal data packet -> forward metrics to ingest service
        Map<String, Object> payload = packet.getPayload();
        ingestService.process(device.getMacAddress(), device.getDevEui(), Instant.now(), payload);
        return Result.DATA;
    }
}

