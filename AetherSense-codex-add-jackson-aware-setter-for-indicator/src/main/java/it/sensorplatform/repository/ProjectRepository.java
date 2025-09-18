package it.sensorplatform.repository;

import org.springframework.data.repository.CrudRepository;

import it.sensorplatform.model.Project;

public interface ProjectRepository extends CrudRepository<Project, Long>{
	
	/*@Query("SELECT p FROM Project p WHERE p.name = :name")	
	public Project getProjectByName(@Param("name") String name);*/
	
	public Project findByName(String name);
	
}
