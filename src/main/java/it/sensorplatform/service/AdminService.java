package it.sensorplatform.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.Admin;
import it.sensorplatform.repository.AdminRepository;
import jakarta.transaction.Transactional;

@Service
public class AdminService {

        @Autowired
        private AdminRepository adminRepository;

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
}

