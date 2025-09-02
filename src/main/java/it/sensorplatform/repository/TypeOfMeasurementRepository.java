package it.sensorplatform.repository;

import org.springframework.data.repository.CrudRepository;

import it.sensorplatform.model.TypeOfDevice;

public interface TypeOfMeasurementRepository extends CrudRepository<TypeOfDevice, Long>{

}
