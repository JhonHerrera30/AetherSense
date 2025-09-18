package it.sensorplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.TypeOfDevice;
import it.sensorplatform.repository.TypeOfMeasurementRepository;

@Service
public class TypeOfMeasurementService {
	@Autowired
	private TypeOfMeasurementRepository typeOfMeasurementRepository;
	
	public TypeOfDevice getTypeOfMeasurementById(Long id) {
		return typeOfMeasurementRepository.findById(id).get();
	}
	
	public Iterable <TypeOfDevice> getAllTypeOfMeasurements(){
		return typeOfMeasurementRepository.findAll();
	}
	
	public TypeOfDevice saveTypeOfMeasurement(TypeOfDevice t) {
		return typeOfMeasurementRepository.save(t);
	}
	
	public void deleteTypeOfMeasurementById(Long id) {
		typeOfMeasurementRepository.deleteById(id);
		
	}
}
