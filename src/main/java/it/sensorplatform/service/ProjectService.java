package it.sensorplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.Project;
import it.sensorplatform.repository.ProjectRepository;

@Service
public class ProjectService {
	
	@Autowired 
	private ProjectRepository projectRepository;
	
	public Project getProjectByName(String name) {
		return this.projectRepository.findByName(name);
	}
	
	public Project getProjectById(Long id) {
		return this.projectRepository.findById(id).get();
	}
	
	public Iterable<Project> getAllProjects(){
		return this.projectRepository.findAll();
	}
	
	
	
}
