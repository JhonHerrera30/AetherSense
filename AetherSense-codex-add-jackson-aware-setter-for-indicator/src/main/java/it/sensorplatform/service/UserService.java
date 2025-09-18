package it.sensorplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.User;
import it.sensorplatform.repository.UserRepository;

@Service
public class UserService {
	@Autowired
	private UserRepository userRepository;
	
	public User getUserById(Long id) {
		return userRepository.findById(id).get();
	}
	
	public Iterable <User> getAllUsers(){
		return userRepository.findAll();
	}
	
	public User saveUser(User u) {
		return userRepository.save(u);
	}
	
	public void deleteUserById(Long id) {
		userRepository.deleteById(id);
		
	}
}
