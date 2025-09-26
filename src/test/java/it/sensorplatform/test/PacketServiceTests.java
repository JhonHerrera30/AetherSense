package it.sensorplatform.test;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Project;
import it.sensorplatform.repository.DeviceRepository;
import it.sensorplatform.repository.ProjectRepository;
import it.sensorplatform.service.IngestService;
import it.sensorplatform.service.PacketService;
import it.sensorplatform.util.MacAddressUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;

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
        Device saved = deviceRepository.findByMacAddress(MacAddressUtils.normalize("AA:BB:CC")).orElseThrow();
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
        device.setActivated(true);
        device.setLatitude(0d);
        device.setLongitude(0d);
        device.setProject(project);
        deviceRepository.save(device);

        PacketDTO dto = new PacketDTO();
        dto.setMacAddress("AA:DD:EE");
        dto.setPayload(Map.of("temp", 22));

        PacketService.Result res = packetService.handlePacket(dto);
        assertEquals(PacketService.Result.DATA, res);
        assertEquals(1, ingestService.last(MacAddressUtils.normalize("AA:DD:EE"), 1).size());
    }

    @Test
    void mergesIndicatorPayloadIntoMetrics() {
        Project project = new Project();
        project.setName("demo-indicator");
        projectRepository.save(project);

        Device device = new Device();
        device.setName("indicatorDevice");
        device.setMacAddress("AA:11:22");
        device.setEmailOwner("");
        device.setActivated(true);
        device.setLatitude(0d);
        device.setLongitude(0d);
        device.setProject(project);
        deviceRepository.save(device);

        PacketDTO dto = new PacketDTO();
        dto.setMacAddress("AA:11:22");
        dto.setIndicator(List.of("fan_err:Fan Error"));
        dto.setIndicatorPayload(Map.of("fan_err", 1));

        PacketService.Result res = packetService.handlePacket(dto);

        assertEquals(PacketService.Result.DATA, res);
        List<IngestService.IndicatorSample> indicators = ingestService
                .last(MacAddressUtils.normalize("AA:11:22"), 1)
                .stream()
                .findFirst()
                .map(IngestService.Sample::indicators)
                .orElse(List.of());

        assertEquals(1, indicators.size());
        assertEquals("fan_err", indicators.get(0).key());
        assertEquals(1, indicators.get(0).value());
    }

    @Test
    void notifiesActivationWithoutAutomaticallyActivatingDevice() {
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
        dto.setLatitude(45.0);
        dto.setLongitude(7.0);

        PacketService.Result res = packetService.handlePacket(dto);
        assertEquals(PacketService.Result.ACTIVATION, res);

        Device updated = deviceRepository.findByMacAddress(MacAddressUtils.normalize("AA:FF:00")).orElseThrow();
        assertFalse(updated.isActivated());
        assertEquals(0d, updated.getLatitude());
        assertEquals(0d, updated.getLongitude());
    }
}

