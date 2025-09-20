package it.sensorplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.dto.PersonalInfoForm;
import it.sensorplatform.model.Credentials;
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

        public User updatePersonalInfo(Credentials credentials, PersonalInfoForm form) {
                if (credentials == null || form == null) {
                        throw new IllegalArgumentException("Credentials and form must not be null");
                }

                User user = credentials.getUser();
                if (user == null) {
                        user = new User();
                }

                user.setName(form.getName());
                user.setSurname(form.getSurname());
                user.setDateOfBirth(form.getDateOfBirth());
                user.setPhoneNumber(form.getPhoneNumber());

                User savedUser = this.saveUser(user);
                credentials.setUser(savedUser);
                return savedUser;
        }
	
	public void deleteUserById(Long id) {
		userRepository.deleteById(id);
		
	}
}
