package it.sensorplatform.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.sensorplatform.model.Indicator;

@Repository
public interface IndicatorRepository extends CrudRepository<Indicator, Long> {

        Optional<Indicator> findByKey(String key);
}
