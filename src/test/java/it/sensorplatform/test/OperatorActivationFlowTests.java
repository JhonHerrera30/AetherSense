package it.sensorplatform.test;

import it.sensorplatform.dto.OperatorActivationNotification;
import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Admin;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Project;
import it.sensorplatform.repository.AdminRepository;
import it.sensorplatform.repository.CredentialsRepository;
import it.sensorplatform.repository.DeviceRepository;
import it.sensorplatform.repository.ProjectRepository;
import it.sensorplatform.service.AdminService;
import it.sensorplatform.service.OperatorActivationService;
import it.sensorplatform.service.PacketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OperatorActivationFlowTests {

    @Autowired
    private PacketService packetService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private OperatorActivationService operatorActivationService;

    @Test
    void publishesNotificationsOnlyToAuthorizedOperators() {
        Project project = new Project();
        project.setName("activation-demo");
        projectRepository.save(project);

        Credentials adminCredentials = new Credentials();
        adminCredentials.setUsername("admin-activation");
        adminCredentials.setEmail("admin.activation@example.com");
        adminCredentials.setPassword("pass");
        adminCredentials.setRole("ADMIN");
        adminCredentials.setProjectId(project.getId());
        credentialsRepository.save(adminCredentials);

        Admin admin = new Admin();
        admin.setCredentials(adminCredentials);
        adminRepository.save(admin);

        Credentials authorizedOperator = new Credentials();
        authorizedOperator.setUsername("authorized-operator");
        authorizedOperator.setEmail("authorized@example.com");
        authorizedOperator.setPassword("pass");
        authorizedOperator.setRole("OPERATOR");
        authorizedOperator.setProjectId(project.getId());
        credentialsRepository.save(authorizedOperator);

        Credentials unauthorizedOperator = new Credentials();
        unauthorizedOperator.setUsername("unauthorized-operator");
        unauthorizedOperator.setEmail("unauthorized@example.com");
        unauthorizedOperator.setPassword("pass");
        unauthorizedOperator.setRole("OPERATOR");
        unauthorizedOperator.setProjectId(project.getId());
        credentialsRepository.save(unauthorizedOperator);

        adminService.authorizeOperator(admin.getId(), authorizedOperator.getId());

        Device device = new Device();
        device.setName("pending-device");
        device.setMacAddress("AA:BB:CC:DD:EE:01");
        device.setDevEui("AABBCCDDEE0001");
        device.setActivated(false);
        device.setEmailOwner(adminCredentials.getEmail());
        device.setProject(project);
        deviceRepository.save(device);

        PacketDTO packet = new PacketDTO();
        packet.setMacAddress("AA:BB:CC:DD:EE:01");
        packet.setProjectId(project.getId());
        packet.setLatitude(45.1);
        packet.setLongitude(7.2);

        PacketService.Result result = packetService.handlePacket(packet);
        assertEquals(PacketService.Result.ACTIVATION, result);

        List<OperatorActivationNotification> authorizedNotifications =
                operatorActivationService.listForOperator(project.getId(), authorizedOperator.getId());
        assertEquals(1, authorizedNotifications.size());
        OperatorActivationNotification notification = authorizedNotifications.get(0);
        assertEquals(device.getId(), notification.getDeviceId());
        assertEquals(adminCredentials.getEmail(), notification.getAdminEmail());
        assertEquals(project.getId(), notification.getProjectId());

        List<OperatorActivationNotification> unauthorizedNotifications =
                operatorActivationService.listForOperator(project.getId(), unauthorizedOperator.getId());
        assertTrue(unauthorizedNotifications.isEmpty());

        OperatorActivationNotification consumed = operatorActivationService.consume(
                project.getId(), device.getId(), authorizedOperator.getId());
        assertNotNull(consumed);
        assertTrue(operatorActivationService.listForOperator(project.getId(), authorizedOperator.getId()).isEmpty());
    }
}
