package it.sensorplatform.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;

public interface DeviceRepository extends CrudRepository<Device, Long>{
	
	public boolean existsByMacAddress (String macAddress);
	
	public Set<Device> findAllByEmailOwnerAndProjectId(String email, Long id);

	public Set<Device> findAllByProjectId(Long projectId);

	public Optional<Device> findByMacAddress(String macAddress);
	
	public Set<Device> findByNameStartingWithIgnoreCase(String deviceInfo);

    public Set<Device> findByMacAddressStartingWithIgnoreCase(String deviceInfo);

    public Set<Device> findByEmailOwnerStartingWithIgnoreCase(String deviceQuery);

    public Set<Device> findByTod_NameStartingWithIgnoreCase(String ndeviceQueryame);
    
    public Set<Device> findAllByOperator(Credentials operator);
	
}