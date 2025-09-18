package it.sensorplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.TypeOfDevice;
import it.sensorplatform.repository.TypeOfDeviceRepository;

@Service
public class TypeOfDeviceService {
	
	@Autowired
	private TypeOfDeviceRepository todRepository;

	public Iterable<TypeOfDevice> findAll() {
		return this.todRepository.findAll();
	}

	public void save(TypeOfDevice tod) {
		this.todRepository.save(tod);
	}
}
