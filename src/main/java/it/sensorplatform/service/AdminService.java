package it.sensorplatform.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.Admin;
import it.sensorplatform.repository.AdminRepository;
import it.sensorplatform.repository.CredentialsRepository;
import it.sensorplatform.model.Credentials;
import jakarta.transaction.Transactional;

@Service
public class AdminService {

        @Autowired
        private AdminRepository adminRepository;

        @Autowired
        private CredentialsRepository credentialsRepository;

        @Transactional
        public Admin getAdmin(Long id) {
                Optional<Admin> result = adminRepository.findById(id);
                return result.orElse(null);
        }

        @Transactional
        public Admin saveAdmin(Admin admin) {
                return adminRepository.save(admin);
        }

        public Iterable<Admin> getAllAdmins() {
                return adminRepository.findAll();
        }

        @Transactional
        public Admin authorizeOperator(Long adminId, Long operatorId) {
                Admin admin = adminRepository.findById(adminId).orElseThrow();
                Credentials operator = credentialsRepository.findById(operatorId).orElseThrow();

                if (admin.getAuthorizedOperators() == null) {
                        admin.setAuthorizedOperators(new ArrayList<>());
                }

                if (!admin.getAuthorizedOperators().contains(operator)) {
                        admin.getAuthorizedOperators().add(operator);
                        adminRepository.save(admin);
                }

                return admin;
        }

        @Transactional
        public Admin removeAuthorizedOperator(Long adminId, Long operatorId) {
                Admin admin = adminRepository.findById(adminId).orElseThrow();
                Credentials operator = credentialsRepository.findById(operatorId).orElseThrow();

                if (admin.getAuthorizedOperators() != null && admin.getAuthorizedOperators().remove(operator)) {
                        adminRepository.save(admin);
                }

                return admin;
        }
}

