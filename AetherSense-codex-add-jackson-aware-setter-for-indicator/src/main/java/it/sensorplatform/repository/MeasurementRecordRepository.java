package it.sensorplatform.repository;

import org.springframework.data.repository.CrudRepository;

import it.sensorplatform.model.MeasurementRecord;

public interface MeasurementRecordRepository extends CrudRepository<MeasurementRecord, Long>{

}
