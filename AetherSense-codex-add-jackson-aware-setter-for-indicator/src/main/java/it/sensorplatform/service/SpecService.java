package it.sensorplatform.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.Spec;
import it.sensorplatform.repository.SpecRepository;

@Service
public class SpecService {
        @Autowired
        private SpecRepository specRepository;

        public List<Spec> findAll() {
                return (List<Spec>) specRepository.findAll();
        }

        public Spec save(Spec spec) {
                return specRepository.save(spec);
        }

        public boolean existsByFields(Spec spec) {
                return specRepository.existsByFields(spec);
        }

        public Optional<Spec> findByFields(Spec spec) {
                return specRepository.findByFields(spec);
        }

        public List<Spec> findAllById(List<Long> specsId) {
                return (List<Spec>) specRepository.findAllById(specsId);
        }

        public void deleteSpecById(Long specId) {
                this.specRepository.deleteById(specId);
        }

        public Spec findById(Long specId) {
                return this.specRepository.findById(specId).get();
        }




}
