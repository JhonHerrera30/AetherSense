package it.sensorplatform.controller.rest;

import it.sensorplatform.dto.UnknownDeviceNotification;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Project;
import it.sensorplatform.model.Spec;
import it.sensorplatform.model.TypeOfDevice;
import it.sensorplatform.repository.DeviceRepository;
import it.sensorplatform.repository.ProjectRepository;
import it.sensorplatform.repository.TypeOfDeviceRepository;
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

@RestController
@RequestMapping("/api/notifications")
public class NotificationControllerRest {
    private final UnknownDeviceService unknownDeviceService;
    private final DeviceRepository deviceRepository;
    private final TypeOfDeviceRepository typeOfDeviceRepository;
    private final SpecService specService;
    private final ProjectRepository projectRepository;
    private static final Logger logger = LoggerFactory.getLogger(NotificationControllerRest.class);

    public NotificationControllerRest(UnknownDeviceService unknownDeviceService,
                                      DeviceRepository deviceRepository,
                                      TypeOfDeviceRepository typeOfDeviceRepository,
                                      SpecService specService,
                                      ProjectRepository projectRepository) {
        this.unknownDeviceService = unknownDeviceService;
        this.deviceRepository = deviceRepository;
        this.typeOfDeviceRepository = typeOfDeviceRepository;
        this.specService = specService;
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
        TypeOfDevice tod = typeOfDeviceRepository.findByName(notif.getTypeOfDevice()).orElse(null);
        if (tod == null) {
            tod = new TypeOfDevice();
            tod.setName(notif.getTypeOfDevice());
            List<Spec> specs = new ArrayList<>();
            if (notif.getSpec() != null) {
                for (String specEntry : notif.getSpec()) {
                    String[] parts = specEntry.split("-");
                    Spec spec = new Spec();
                    spec.setComponent(parts.length > 0 ? parts[0] : "");
                    spec.setMeasurement(parts.length > 1 ? parts[1] : "");
                    spec.setUnitOfMeasurement(parts.length > 2 ? parts[2] : "");
                    if (!specService.existsByFields(spec)) {
                        specService.save(spec);
                    }
                    specs.add(spec);
                }
            }
            tod.setSpecs(specs);
            typeOfDeviceRepository.save(tod);
        }

        String macAddress = MacAddressUtils.normalize(notif.getMacAddress());
        if (macAddress == null || macAddress.isBlank()) {
            macAddress = MacAddressUtils.normalize(notif.getDevEui());
            logger.info("MAC address missing; using DevEUI {} as MAC address", macAddress);
        }

        String normalizedDevEui = MacAddressUtils.normalize(notif.getDevEui());
        if (deviceRepository.existsByMacAddress(macAddress)
                || (normalizedDevEui != null && deviceRepository.existsByDevEui(normalizedDevEui))) {
            logger.warn("Device with MAC {} or DevEUI {} already exists", macAddress, notif.getDevEui());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        logger.info("Creating device with MAC {} and DevEUI {}", macAddress, notif.getDevEui());
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
}
