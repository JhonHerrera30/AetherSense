package it.sensorplatform.service;

import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Superadmin;
import it.sensorplatform.repository.SuperadminRepository;

@Service
public class SuperadminService {

        @Autowired
        private SuperadminRepository superadminRepository;

        public Superadmin findByCredentials(Credentials credentials) {
                if (credentials == null) {
                        return null;
                }
                return this.superadminRepository.findByCredentials(credentials).orElse(null);
        }

        public Superadmin save(Superadmin superadmin) {
                return this.superadminRepository.save(superadmin);
        }

        public Superadmin getDefaultSuperadmin() {
                Iterator<Superadmin> iterator = this.superadminRepository.findAll().iterator();
                if (iterator.hasNext()) {
                        return iterator.next();
                }
                return null;
        }
}
