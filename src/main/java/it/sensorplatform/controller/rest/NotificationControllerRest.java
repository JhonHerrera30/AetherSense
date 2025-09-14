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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationControllerRest {
    private final UnknownDeviceService unknownDeviceService;
    private final DeviceRepository deviceRepository;
    private final TypeOfDeviceRepository typeOfDeviceRepository;
    private final SpecService specService;
    private final ProjectRepository projectRepository;

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
        UnknownDeviceNotification notif = unknownDeviceService.consume(projectId, key);
        if (notif == null) {
            return ResponseEntity.notFound().build();
        }
        Project project = projectRepository.findById(projectId).orElse(null);
        TypeOfDevice tod = typeOfDeviceRepository.findByName(notif.getTypeOfDevice()).orElse(null);
        if (tod == null) {
            tod = new TypeOfDevice();
            tod.setName(notif.getTypeOfDevice());
            List<Spec> specs = new ArrayList<>();
            for (Map.Entry<String, Object> entry : notif.getPayload().entrySet()) {
                String[] parts = entry.getKey().split("[_:]");
                Spec spec = new Spec();
                spec.setMeasurement(parts[0]);
                spec.setComponent(parts.length > 1 ? parts[1] : "");
                spec.setUnitOfMeasurement("");
                if (!specService.existsByFields(spec)) {
                    specService.save(spec);
                }
                specs.add(spec);
            }
            tod.setSpecs(specs);
            typeOfDeviceRepository.save(tod);
        }
        Device device = new Device();
        device.setMacAddress(notif.getMacAddress());
        device.setDevEui(notif.getDevEui());
        device.setProject(project);
        device.setTod(tod);
        device.setStatus("deactivated");
        deviceRepository.save(device);
        return ResponseEntity.ok().build();
    }
}
