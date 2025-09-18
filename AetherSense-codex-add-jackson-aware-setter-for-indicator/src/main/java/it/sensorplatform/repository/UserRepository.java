package it.sensorplatform.repository;

import org.springframework.data.repository.CrudRepository;

import it.sensorplatform.model.User;

public interface UserRepository extends CrudRepository<User, Long> {

}
