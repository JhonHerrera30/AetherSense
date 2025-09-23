package it.sensorplatform.service;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Device;
import it.sensorplatform.repository.DeviceRepository;
import it.sensorplatform.util.MacAddressUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Service that applies the device/project filter logic described in the
 * specification. It handles three cases:
 *  1. unknown device -> notify the unknown device service
 *  2. known device not yet activated -> perform the activation flow
 *  3. activated device -> forward data to IngestService
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
        String mac = MacAddressUtils.normalize(packet.getMacAddress());
        String devEui = MacAddressUtils.normalize(packet.getDevEui());
        if ((mac == null || mac.isBlank()) &&
                (devEui == null || devEui.isBlank())) {
            throw new IllegalArgumentException("macAddress or devEui is required");
        }

        Optional<Device> existing = Optional.empty();
        if (mac != null && !mac.isBlank()) {
            existing = deviceRepository.findByMacAddress(mac);
        }
        if (existing.isEmpty() && devEui != null && !devEui.isBlank()) {
            existing = deviceRepository.findByDevEui(devEui);
        }
        if (existing.isEmpty()) {
            System.out.println("PacketService.handlePacket - device not found, notifying UnknownDeviceService");
            unknownDeviceService.notify(packet);
            return Result.NEW_DEVICE;
        }

        Device device = existing.get();

        if (!device.isActivated()) {
            System.out.println("PacketService.handlePacket - activation packet for device: " + device.getId());
            // Case 2: activation packet -> update flags and location
            if (packet.getLatitude() != null) device.setLatitude(packet.getLatitude());
            if (packet.getLongitude() != null) device.setLongitude(packet.getLongitude());
            device.setActivated(true);
            deviceRepository.save(device);
            return Result.ACTIVATION;
        }

        System.out.println("PacketService.handlePacket - forwarding data to IngestService for device: " + device.getMacAddress());
        // Case 3: normal data packet -> forward metrics to ingest service
        Map<String, Object> payload = packet.getPayload();
        ingestService.process(
                device,
                Instant.now(),
                payload,
                packet.getSpec(),
                packet.getIndicator()
        );
        return Result.DATA;
    }
}

