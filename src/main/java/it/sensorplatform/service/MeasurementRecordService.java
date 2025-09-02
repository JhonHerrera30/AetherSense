package it.sensorplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.MeasurementRecord;
import it.sensorplatform.repository.MeasurementRecordRepository;

@Service
public class MeasurementRecordService {
	@Autowired
	private MeasurementRecordRepository measurementRecordRepository;
	
	public MeasurementRecord getMeasurementRecordById(Long id) {
		return measurementRecordRepository.findById(id).get();
	}
	
	public Iterable <MeasurementRecord> getAllMeasurementRecords(){
		return measurementRecordRepository.findAll();
	}
	
	public MeasurementRecord saveMeasurementRecord(MeasurementRecord m) {
		return measurementRecordRepository.save(m);
	}
	
	public void deleteMeasurementRecordById(Long id) {
		measurementRecordRepository.deleteById(id);
		
	}
}
