package it.sensorplatform.test;

import it.sensorplatform.model.Admin;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.repository.AdminRepository;
import it.sensorplatform.repository.CredentialsRepository;
import it.sensorplatform.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminServiceTests {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Test
    void authorizeAndRemoveOperator() {
        Credentials adminCred = new Credentials();
        adminCred.setUsername("admin1");
        adminCred.setEmail("admin@example.com");
        adminCred.setPassword("pass");
        adminCred.setRole("ROLE_ADMIN");
        credentialsRepository.save(adminCred);

        Admin admin = new Admin();
        admin.setCredentials(adminCred);
        adminRepository.save(admin);

        Credentials operatorCred = new Credentials();
        operatorCred.setUsername("operator1");
        operatorCred.setEmail("op@example.com");
        operatorCred.setPassword("pass");
        operatorCred.setRole("ROLE_OP");
        credentialsRepository.save(operatorCred);

        adminService.authorizeOperator(admin.getId(), operatorCred.getId());
        Admin updated = adminService.getAdmin(admin.getId());
        assertTrue(updated.getAuthorizedOperators().contains(operatorCred));

        adminService.removeAuthorizedOperator(admin.getId(), operatorCred.getId());
        Admin updatedAfterRemoval = adminService.getAdmin(admin.getId());
        assertFalse(updatedAfterRemoval.getAuthorizedOperators().contains(operatorCred));
    }
}
