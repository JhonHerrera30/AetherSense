package it.sensorplatform.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.sensorplatform.model.Admin;

@Repository
public interface AdminRepository extends CrudRepository<Admin, Long> {
}

