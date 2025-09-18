package it.sensorplatform.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Group;
import it.sensorplatform.model.Project;
import it.sensorplatform.repository.GroupRepository;

@Service
public class GroupService {
	@Autowired 
	private GroupRepository groupRepository;
	
	@Autowired 
	private DeviceService deviceService;
	
	public Iterable<Group> getAllGroups(){
		return groupRepository.findAll();
	}
	
	public Set <Group> findAllByCredentials(Credentials credentials){
		return groupRepository.findAllByCredentials(credentials);
	}
	
	public Group findGroupById(Long id) {
		return this.groupRepository.findById(id).get();
	}
	
	public Group findGroupByNameAndCredentials(String name, Credentials credentials) {
		Optional<Group> group = this.groupRepository.findByNameAndCredentials(name, credentials);
		Group g;
		if (group.isPresent()) {
		    g = group.get();
		}
		else
			g = null;
		return g;
	}

	public Set<Group> findByNameStartingWithIgnoreCaseAndCredentials(String groupName, Credentials credentials) {
		return groupRepository.findByNameStartingWithIgnoreCaseAndCredentials(groupName, credentials);
	}

	public void deleteById(Long groupId) {
		this.groupRepository.deleteById(groupId);
	}

	public void save(Group group) {
		this.groupRepository.save(group);
	}

	public List<Device> getAvailableDevicesForGroup(Long groupId, Credentials userCreds) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        Project project = group.getProject();
        Set<Device> allUserDevices = deviceService.findAllByEmailAndProjectId(userCreds.getEmail(), project.getId());
        Set<Device> assigned = new HashSet<>(group.getDevices());

        return allUserDevices.stream()
            .filter(device -> !assigned.contains(device))
            .toList();
    }
}
