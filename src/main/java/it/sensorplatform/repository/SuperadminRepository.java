package it.sensorplatform.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Superadmin;

@Repository
public interface SuperadminRepository extends CrudRepository<Superadmin, Long> {

        Optional<Superadmin> findByCredentials(Credentials credentials);
}
