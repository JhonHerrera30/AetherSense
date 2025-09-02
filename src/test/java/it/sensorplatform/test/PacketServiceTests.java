package it.sensorplatform.test;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Project;
import it.sensorplatform.repository.DeviceRepository;
import it.sensorplatform.repository.ProjectRepository;
import it.sensorplatform.service.IngestService;
import it.sensorplatform.service.PacketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PacketServiceTests {

    @Autowired
    private PacketService packetService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IngestService ingestService;

    @Test
    void registersNewDeviceWhenUnknown() {
        Project project = new Project();
        project.setName("demo");
        projectRepository.save(project);

        PacketDTO dto = new PacketDTO();
        dto.setMacAddress("AA:BB:CC");
        dto.setProjectId(project.getId());
        PacketService.Result res = packetService.handlePacket(dto);

        assertEquals(PacketService.Result.NEW_DEVICE, res);
        Device saved = deviceRepository.findByMacAddress("AA:BB:CC").orElseThrow();
        assertFalse(saved.isActivated());
        assertEquals(project.getId(), saved.getProject().getId());
    }

    @Test
    void forwardsDataForKnownDevice() {
        Project project = new Project();
        project.setName("demo");
        projectRepository.save(project);

        Device device = new Device();
        device.setName("d1");
        device.setMacAddress("AA:DD:EE");
        device.setEmailOwner("");
        device.setActivated(false);
        device.setLatitude(0d);
        device.setLongitude(0d);
        device.setProject(project);
        deviceRepository.save(device);

        PacketDTO dto = new PacketDTO();
        dto.setMacAddress("AA:DD:EE");
        dto.setPayload(Map.of("temp", 22));

        PacketService.Result res = packetService.handlePacket(dto);
        assertEquals(PacketService.Result.DATA, res);
        assertEquals(1, ingestService.last("AA:DD:EE", 1).size());
    }

    @Test
    void activatesDeviceWhenFlagPresent() {
        Project project = new Project();
        project.setName("demo");
        projectRepository.save(project);

        Device device = new Device();
        device.setName("d1");
        device.setMacAddress("AA:FF:00");
        device.setEmailOwner("");
        device.setActivated(false);
        device.setLatitude(0d);
        device.setLongitude(0d);
        device.setProject(project);
        deviceRepository.save(device);

        PacketDTO dto = new PacketDTO();
        dto.setMacAddress("AA:FF:00");
        dto.setActivation(true);
        dto.setLatitude(45.0);
        dto.setLongitude(7.0);

        PacketService.Result res = packetService.handlePacket(dto);
        assertEquals(PacketService.Result.ACTIVATION, res);

        Device updated = deviceRepository.findByMacAddress("AA:FF:00").orElseThrow();
        assertTrue(updated.isActivated());
        assertEquals(45.0, updated.getLatitude());
        assertEquals(7.0, updated.getLongitude());
    }
}

