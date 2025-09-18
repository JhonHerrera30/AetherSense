package it.sensorplatform.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.Indicator;
import it.sensorplatform.repository.IndicatorRepository;

@Service
public class IndicatorService {

        @Autowired
        private IndicatorRepository indicatorRepository;

        public List<Indicator> findAll() {
                return (List<Indicator>) indicatorRepository.findAll();
        }

        public Optional<Indicator> findByKey(String key) {
                return indicatorRepository.findByKey(key);
        }

        public Indicator save(Indicator indicator) {
                return indicatorRepository.save(indicator);
        }
}
