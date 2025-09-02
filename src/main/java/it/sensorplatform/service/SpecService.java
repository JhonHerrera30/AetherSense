package it.sensorplatform.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import it.sensorplatform.model.Spec;

import it.sensorplatform.repository.SpecRepository;

@Service
public class SpecService {
	@Autowired
	private SpecRepository specRepository;

	public List<Spec> findAll() {
		return (List<Spec>) specRepository.findAll();
	}

	public void save(Spec spec) {
		specRepository.save(spec);
	}
	
	public boolean existsByFields(Spec spec) {
		return specRepository.existsByFields(spec);
	}

	public List<Spec> findAllById(List<Long> specsId) {
		return (List<Spec>) specRepository.findAllById(specsId);
	}

	public void deleteSpecById(Long specId) {
		this.specRepository.deleteById(specId);
	}

	public Spec findById(Long specId) {
		return this.specRepository.findById(specId).get();
	}

	
	
	
}
