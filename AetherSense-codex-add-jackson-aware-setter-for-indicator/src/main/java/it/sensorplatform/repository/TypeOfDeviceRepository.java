package it.sensorplatform.repository;

import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

import it.sensorplatform.model.TypeOfDevice;

public interface TypeOfDeviceRepository extends CrudRepository<TypeOfDevice, Long>{

        Optional<TypeOfDevice> findByName(String name);

}
