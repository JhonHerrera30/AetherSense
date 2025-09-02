package it.sensorplatform.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.repository.DeviceRepository;

@Service
public class DeviceService {

	@Autowired
	private DeviceRepository deviceRepository;


	public Set<Device> findAllByEmailAndProjectId(String email, Long id) {
		return this.deviceRepository.findAllByEmailOwnerAndProjectId(email, id);
	}

	public Set<Device> findAllByProjectId(Long projectId) {
		return this.deviceRepository.findAllByProjectId(projectId);
	}

	public void deleteById(Long deviceId) {
		this.deviceRepository.deleteById(deviceId);
	}

	public void saveDevice(Device existing) {
		this.deviceRepository.save(existing);
	}

	public Device findById(Long deviceId) {
		return this.deviceRepository.findById(deviceId).get();
	}

	public Device findByMacAddress(String macAddress) {
		return this.deviceRepository.findByMacAddress(macAddress).get();
	}

	public void save(Device device) {
		this.deviceRepository.save(device);
	}

	public boolean existsByMacAddress(String macAddress) {
		return this.deviceRepository.existsByMacAddress(macAddress);
	}

	public Set<Device> findByNameStartingWithIgnoreCase(String deviceInfo) {
        return this.deviceRepository.findByNameStartingWithIgnoreCase(deviceInfo);
    }

    public Set<Device> findByMacAddressStartingWithIgnoreCase(String deviceInfo) {
        return this.deviceRepository.findByMacAddressStartingWithIgnoreCase(deviceInfo);
    }

    public Set<Device> findByEmailOwnerStartingWithIgnoreCase(String deviceQuery) {
        return this.deviceRepository.findByEmailOwnerStartingWithIgnoreCase(deviceQuery);
    }

    public Set<Device> findByTod_NameStartingWithIgnoreCase(String deviceQuery) {
        return this.deviceRepository.findByTod_NameStartingWithIgnoreCase(deviceQuery);
    }

	public void delete(Device device) {
		this.deviceRepository.delete(device);		
	}
	
	public List<Device> findAll(){
		return (List<Device>) this.deviceRepository.findAll();
	}
	
    public Set<Device> findAllByOperator(Credentials operator) {
        return this.deviceRepository.findAllByOperator(operator);
    }
}