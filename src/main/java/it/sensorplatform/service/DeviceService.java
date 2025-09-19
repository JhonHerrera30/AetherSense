package it.sensorplatform.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.repository.DeviceRepository;
import it.sensorplatform.util.MacAddressUtils;

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
                if (existing != null) {
                        existing.setMacAddress(MacAddressUtils.normalize(existing.getMacAddress()));
                        this.deviceRepository.save(existing);
                }
        }

        public Device findById(Long deviceId) {
                return this.deviceRepository.findById(deviceId).get();
        }

        public Device findByMacAddress(String macAddress) {
                String normalizedMac = MacAddressUtils.normalize(macAddress);
                if (normalizedMac == null || normalizedMac.isBlank()) {
                        throw new IllegalArgumentException("MAC address is required");
                }
                return this.deviceRepository.findByMacAddress(normalizedMac)
                                .orElseThrow(() -> new IllegalArgumentException("Device not found for key: " + normalizedMac));
        }

        public Optional<Device> findOptionalByMacAddress(String macAddress) {
                String normalizedMac = MacAddressUtils.normalize(macAddress);
                if (normalizedMac == null || normalizedMac.isBlank()) {
                        return Optional.empty();
                }
                return this.deviceRepository.findByMacAddress(normalizedMac);
        }

        public Optional<Device> findOptionalByDevEui(String devEui) {
                String normalizedDevEui = MacAddressUtils.normalize(devEui);
                if (normalizedDevEui == null || normalizedDevEui.isBlank()) {
                        return Optional.empty();
                }
                return this.deviceRepository.findByDevEui(normalizedDevEui);
        }

        public Optional<Device> findOptionalByDeviceKey(String deviceKey) {
                String normalizedKey = MacAddressUtils.normalize(deviceKey);
                if (normalizedKey == null || normalizedKey.isBlank()) {
                        return Optional.empty();
                }
                Optional<Device> byMac = this.deviceRepository.findByMacAddress(normalizedKey);
                if (byMac.isPresent()) {
                        return byMac;
                }
                return this.deviceRepository.findByDevEui(normalizedKey);
        }

        public Device findByDeviceKey(String deviceKey) {
                return findOptionalByDeviceKey(deviceKey)
                                .orElseThrow(() -> new IllegalArgumentException("Device not found for key: " + deviceKey));
        }

        public void save(Device device) {
                if (device != null) {
                        device.setMacAddress(MacAddressUtils.normalize(device.getMacAddress()));
                        this.deviceRepository.save(device);
                }
        }

        public boolean existsByMacAddress(String macAddress) {
                String normalizedMac = MacAddressUtils.normalize(macAddress);
                if (normalizedMac == null || normalizedMac.isBlank()) {
                        return false;
                }
                return this.deviceRepository.existsByMacAddress(normalizedMac);
        }

	public Set<Device> findByNameStartingWithIgnoreCase(String deviceInfo) {
        return this.deviceRepository.findByNameStartingWithIgnoreCase(deviceInfo);
    }

    public Set<Device> findByMacAddressStartingWithIgnoreCase(String deviceInfo) {
        String normalizedInfo = MacAddressUtils.normalize(deviceInfo);
        if (normalizedInfo == null || normalizedInfo.isBlank()) {
            return Collections.emptySet();
        }
        return this.deviceRepository.findByMacAddressStartingWithIgnoreCase(normalizedInfo);
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