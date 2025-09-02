package it.sensorplatform.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Group;

@Repository
public interface GroupRepository extends CrudRepository<Group, Long>{
	
	public Set <Group> findAllByCredentials(Credentials credentials);
	
	public Optional <Group> findByNameAndCredentials(String name, Credentials credentials);

	public Set<Group> findByNameStartingWithIgnoreCaseAndCredentials(String groupName, Credentials credentials);
}
