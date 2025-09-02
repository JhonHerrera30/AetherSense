package it.sensorplatform.controller.rest;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import it.sensorplatform.dto.DeviceDTO;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.GroupService;

@RestController
@RequestMapping("/api/groups")
public class GroupControllerRest {

    @Autowired
    private GroupService groupService;
    
    @Autowired
	private CredentialsService credentialsService;

    @GetMapping("/{groupId}/devices")
    public List<DeviceDTO> getDevicesByGroupId(@PathVariable("groupId") Long groupId) {
        List<Device> devices = groupService.findGroupById(groupId).getDevices();
        return devices.stream()
                .map(d -> new DeviceDTO(d.getName(), d.getMacAddress(), d.getEmailOwner(), d.getDevEui(), d.getLongitude(), d.getLatitude(), d.getTod().getName(), d.getVisibleUsername()))
                .collect(Collectors.toList());
    }
    
    @GetMapping("/name/{groupName}/devices")
    public List<DeviceDTO> getDeviceByGroupName(@PathVariable("groupName") String groupName) {
    	UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
        List<Device> devices = groupService.findGroupByNameAndCredentials(groupName, credentials).getDevices();
        return devices.stream()
                .map(d -> new DeviceDTO(d.getName(), d.getMacAddress(), d.getEmailOwner(), d.getDevEui(), d.getLongitude(), d.getLatitude(), d.getTod().getName(), d.getVisibleUsername()))
                .collect(Collectors.toList());
    }
    
    @GetMapping("/{groupId}/available-devices")
    @ResponseBody
    public List<DeviceDTO> getAvailableDevices(@PathVariable Long groupId, Principal principal) {
        Credentials credentials = credentialsService.getCredentials(principal.getName());
        List<Device> devices = groupService.getAvailableDevicesForGroup(groupId, credentials);

        return devices.stream()
            .map(device -> new DeviceDTO(
                device.getName(),
                device.getMacAddress(),
                device.getEmailOwner(),
                device.getDevEui(),
                device.getLongitude(),
                device.getLatitude(),
                device.getTod() != null ? device.getTod().getName() : null,
                device.getVisibleUsername()
            ))
            .toList();
    }
    
    
}