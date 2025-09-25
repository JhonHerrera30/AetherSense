package it.sensorplatform.service;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Admin;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.repository.DeviceRepository;
import it.sensorplatform.util.MacAddressUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.List;

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
    private final OperatorActivationService operatorActivationService;
    private final AdminService adminService;
    private final CredentialsService credentialsService;

    public PacketService(DeviceRepository deviceRepository,
                         IngestService ingestService,
                         UnknownDeviceService unknownDeviceService,
                         OperatorActivationService operatorActivationService,
                         AdminService adminService,
                         CredentialsService credentialsService) {
        this.deviceRepository = deviceRepository;
        this.ingestService = ingestService;
        this.unknownDeviceService = unknownDeviceService;
        this.operatorActivationService = operatorActivationService;
        this.adminService = adminService;
        this.credentialsService = credentialsService;
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

        Double latitude = packet.getLatitude();
        Double longitude = packet.getLongitude();
        boolean hasValidCoordinates = hasValidCoordinate(latitude) && hasValidCoordinate(longitude);
        boolean hasEmailOwner = StringUtils.hasText(device.getEmailOwner());
        boolean deviceUpdated = false;

        if (hasValidCoordinates && hasEmailOwner) {
            if (!device.isActivated()) {
                device.setLatitude(latitude);
                device.setLongitude(longitude);
                device.setActivated(true);
                deviceUpdated = true;
            } else if (coordinatesDiffer(device.getLatitude(), latitude) || coordinatesDiffer(device.getLongitude(), longitude)) {
                device.setLatitude(latitude);
                device.setLongitude(longitude);
                deviceUpdated = true;
            }

            if (deviceUpdated) {
                deviceRepository.save(device);
            }
        }

        if (!device.isActivated()) {
            System.out.println("PacketService.handlePacket - activation packet for device: " + device.getId());
            notifyOperators(device, packet);
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

    private void notifyOperators(Device device, PacketDTO packet) {
        String emailOwner = device.getEmailOwner();
        Long projectId = device.getProject() != null ? device.getProject().getId() : packet.getProjectId();
        if (!StringUtils.hasText(emailOwner) || projectId == null) {
            return;
        }
        Optional<Credentials> credentialsOptional = credentialsService.findByEmailAndProjectId(emailOwner, projectId);
        if (credentialsOptional.isEmpty()) {
            return;
        }
        Credentials adminCredentials = credentialsOptional.get();
        Admin admin = adminCredentials.getAdmin();
        if (admin == null) {
            return;
        }
        Admin managedAdmin = adminService.getAdmin(admin.getId());
        if (managedAdmin == null) {
            return;
        }
        List<Credentials> authorizedOperators = managedAdmin.getAuthorizedOperators();
        if (authorizedOperators == null || authorizedOperators.isEmpty()) {
            return;
        }
        operatorActivationService.notifyActivation(device, packet, managedAdmin, authorizedOperators);
    }

    private boolean hasValidCoordinate(Double value) {
        return value != null && !Double.isNaN(value) && Double.compare(value, 0d) != 0;
    }

    private boolean coordinatesDiffer(Double stored, Double incoming) {
        if (incoming == null) {
            return false;
        }
        return stored == null || Double.compare(stored, incoming) != 0;
    }
}

