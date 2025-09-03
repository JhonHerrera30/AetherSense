package it.sensorplatform.controller;

import it.sensorplatform.dto.DeviceDTO;
import it.sensorplatform.model.Admin;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Project;
import it.sensorplatform.model.User;
import it.sensorplatform.service.AdminService;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.DeviceService;
import it.sensorplatform.service.ProjectService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class DeviceController {

	@Autowired
	private DeviceService deviceService;

	@Autowired
	private ProjectService projectService;

        @Autowired
        private CredentialsService credentialsService;

        @Autowired
        private AdminService adminService;

	@GetMapping("/superadmin/manageProjectDevices/{projectId}")
	public String manageProjectDevices(@PathVariable("projectId") Long projectId,
			@RequestParam(value = "deviceQuery", required = false) String deviceQuery, Model model) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
		model.addAttribute("user", credentials);
		Project project = projectService.getProjectById(projectId);
		Set<Device> devicesFiltred;
		if (deviceQuery != null && !deviceQuery.isEmpty()) {
			devicesFiltred = deviceService.findByNameStartingWithIgnoreCase(deviceQuery);
			devicesFiltred.addAll(deviceService.findByMacAddressStartingWithIgnoreCase(deviceQuery));
			devicesFiltred.addAll(deviceService.findByEmailOwnerStartingWithIgnoreCase(deviceQuery));
			devicesFiltred.addAll(deviceService.findByTod_NameStartingWithIgnoreCase(deviceQuery));
		} else {
			devicesFiltred = deviceService.findAllByProjectId(projectId);
		}
		List<Device> devices = new ArrayList<>(devicesFiltred);
		this.loadDeviceDTO(devices, model);
		model.addAttribute("project", project);
		return "superadmin/manageProjectDevices.html";
	}

	@GetMapping("/superadmin/formNewDevice/{projectId}")
	public String formNewDevice(@PathVariable("projectId") Long projectId, Model model) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
		model.addAttribute("user", credentials);
		Project project = projectService.getProjectById(projectId);
		Device device = new Device();
		device.setProject(project);
		model.addAttribute("device", device);
		model.addAttribute("project", project);
		model.addAttribute("tods", project.getTods());
		return "superadmin/formNewDevice.html";
	}

	@PostMapping("/superadmin/newDevice/{projectId}")
	public String saveDevice(@Valid @ModelAttribute("device") Device device, BindingResult bindingResult,
			@PathVariable("projectId") Long projectId, Model model, RedirectAttributes redirectAttributes) {

		Project project = projectService.getProjectById(projectId);
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
		model.addAttribute("user", credentials);

		if (bindingResult.hasErrors()) {
			device.setProject(project);
			model.addAttribute("device", device);
			model.addAttribute("project", project);
			model.addAttribute("tods", project.getTods());
			return "superadmin/formNewDevice.html";
		}

		if (device.getTod() == null) {
			device.setProject(project);
			model.addAttribute("device", device);
			model.addAttribute("project", project);
			model.addAttribute("tods", project.getTods());
			model.addAttribute("noTodSelected", "Please choose a type of device.");
			return "superadmin/formNewDevice.html";
		}

		if (deviceService.existsByMacAddress(device.getMacAddress())) {
			model.addAttribute("device", device);
			model.addAttribute("project", project);
			model.addAttribute("tods", project.getTods());
			model.addAttribute("duplicateDeviceError", "A device with this MAC Address already exists");
			return "superadmin/formNewDevice.html";
		}
		device.setProject(project);
		deviceService.saveDevice(device);
		redirectAttributes.addFlashAttribute("successMessage", "Device added successfully!");
		List<Device> devices = new ArrayList<>(deviceService.findAllByProjectId(projectId));
		this.loadDeviceDTO(devices, model);
		model.addAttribute("project", project);
		return "redirect:/superadmin/manageProjectDevices/" + projectId;
	}

	@GetMapping("/superadmin/formUpdateDevice/{projectId}/{macAddress}")
	public String formUpdateDevice(@PathVariable("projectId") Long projectId,
			@PathVariable("macAddress") String macAddress, Model model) {
		Project project = projectService.getProjectById(projectId);
		Device device = deviceService.findByMacAddress(macAddress);
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
		model.addAttribute("user", credentials);
		model.addAttribute("project", project);
		model.addAttribute("device", device);
		return "superadmin/formUpdateDevice.html";
	}

	@PostMapping("/superadmin/updateDevice/{projectId}/{macAddress}")
	public String adminUpdateDevice(@PathVariable("projectId") Long projectId,
			@PathVariable("macAddress") String macAddress, @RequestParam String name, @RequestParam Double latitude,
			@RequestParam Double longitude, RedirectAttributes redirectAttributes) {

		if (name == null || name.trim().isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "Device name is required.");
			return "redirect:/superadmin/manageProjectDevices/" + projectId;
		}

		// Recupera il dispositivo tramite MAC address
		Device device = deviceService.findByMacAddress(macAddress);

		if (device == null) {
			redirectAttributes.addFlashAttribute("error", "Device not found.");
			return "redirect:/superadmin/manageProjectDevices/" + projectId;
		}

		// Aggiorna solo i campi modificabili
		device.setName(name);
		device.setLatitude(latitude);
		device.setLongitude(longitude);

		// Salvataggio
		deviceService.save(device);

		redirectAttributes.addFlashAttribute("success", "Device updated successfully.");
		return "redirect:/superadmin/manageProjectDevices/" + projectId;
	}

	@PostMapping("/superadmin/deleteDevice/{projectId}/{macAddress}")
	public String deleteDevice(@PathVariable("projectId") Long projectId, @PathVariable("macAddress") String macAddress,
			RedirectAttributes redirectAttributes) {
		Device device = deviceService.findByMacAddress(macAddress);
		deviceService.delete(device);
		redirectAttributes.addFlashAttribute("successMessage", "Dispositivo eliminato.");
		return "redirect:/superadmin/manageProjectDevices/" + projectId;
	}

	@GetMapping("/device/{projectId}/{macAddress}")
	public String aboutDevice(@PathVariable("projectId") Long projectId, @PathVariable("macAddress") String macAddress,
			Model model) {
		Project project = projectService.getProjectById(projectId);
		Device device = deviceService.findByMacAddress(macAddress);
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
		model.addAttribute("user", credentials);
		model.addAttribute("project", project);
		model.addAttribute("device", device);
		return "updateDevice";
	}

	@PostMapping("/updateDevice/{projectId}/{macAddress}")
	public String updateDevice(@PathVariable Long projectId, @PathVariable String macAddress, @RequestParam String name,
			@RequestParam Double latitude, @RequestParam Double longitude, RedirectAttributes redirectAttributes) {

		if (name == null || name.trim().isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "Device name is required.");
			return "redirect:/device/" + projectId + "/" + macAddress;
		}

		// Recupera il dispositivo tramite MAC address
		Device device = deviceService.findByMacAddress(macAddress);

		if (device == null) {
			redirectAttributes.addFlashAttribute("error", "Device not found.");
			return "redirect:/device/" + projectId + "/" + macAddress;
		}

		// Aggiorna solo i campi modificabili
		device.setName(name);
		device.setLatitude(latitude);
		device.setLongitude(longitude);

		// Salvataggio
		deviceService.save(device);

		redirectAttributes.addFlashAttribute("success", "Device updated successfully.");
		return "redirect:/device/" + projectId + "/" + macAddress;
	}

	@GetMapping("/admin/formRegisterOperator/{projectId}")
	public String formRegisterOperator(@PathVariable Long projectId, Model model) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
		model.addAttribute("user", credentials);
		// model.addAttribute("user", new User());
		model.addAttribute("credentials", new Credentials());
		model.addAttribute("projectId", projectId);

		return "/admin/formRegisterOperator";

	}

	@PostMapping("/admin/registerOperator/{projectId}")
	public String registerOperator(@PathVariable Long projectId, @Valid Credentials credentials,
			BindingResult bindingResult, @RequestParam("confirmPassword") String confirmPassword, Model model) {
		boolean error = false;
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials userCredentials = credentialsService.getCredentials(userDetails.getUsername());
                model.addAttribute("user", userCredentials);
                model.addAttribute("projectId", projectId);
                Project project = projectService.getProjectById(projectId);
                Admin admin = adminService.getAdmin(userCredentials.getAdmin().getId());
                String email = credentials.getEmail();
                String username = credentials.getUsername();
                String projectName = project.getName();
		credentials.setVisibleUsername(username);
		username = username + "|" + projectName;
		
		if (!bindingResult.hasErrors()) {
			if (!credentials.getPassword().equals(confirmPassword)) {
				error = true;
				model.addAttribute("passwordMismatchError", "Passwords do not match.");
			}
			if (credentialsService.existsByUsername(username)) {
				error = true;
				model.addAttribute("usernameAlreadyInUse", "Username already in use for this project");
			}
			if (credentialsService.existsByEmailAndProjectId(email, projectId)) {
				error = true;
				model.addAttribute("emailAlreadyInUse", "Email already in use for this project");
			}
			if (error) {
				model.addAttribute("projectId", projectId);
//				List<Project> projects = (List<Project>) projectService.getAllProjects();
//				model.addAttribute("projects", projects);
				return "/admin/formRegisterOperator";
			}

			credentials.setUsername(username);
			if (project.getName().equals("LTRAD")) {
				credentials.setRole(Credentials.LTRAD_OPERATOR_ROLE);
			}
			if (project.getName().equals("FIRE")) {
				credentials.setRole(Credentials.FIRE_OPERATOR_ROLE);
			}
                        if (project.getName().equals("VOLCANO")) {
                                credentials.setRole(Credentials.VOLCANO_OPERATOR_ROLE);
                        }
                        credentials.setEmployer(admin);
                        Credentials savedCredentials = credentialsService.saveCredentials(credentials);
                        admin.getOperators().add(savedCredentials);
                        adminService.saveAdmin(admin);
                        model.addAttribute("project", project);
                        model.addAttribute("successMessage", "New operator created succesfully");

			return "redirect:/admin/group/" + projectId;
		}
				
		return "/admin/formRegisterOperator";

	}

	@PostMapping("/admin/selectOperator/{macAddress}/{opId}/{projectId}")
	public String assignOperatorToDevice(@PathVariable ("projectId") Long projectId, @PathVariable ("macAddress") String macAddress, 
										@PathVariable("opId") Long opId, RedirectAttributes ra) {
		Device d = deviceService.findByMacAddress(macAddress);
		Credentials operator = credentialsService.findById(opId);
		
		d.setOperator(operator);
		deviceService.save(d);
		
		return "redirect:/admin/group/"+projectId;
	}
	
	@PostMapping("/admin/removeOperator/{macAddress}/{projectId}")
	public String removeOperatorfromDevice(@PathVariable ("projectId") Long projectId, @PathVariable ("macAddress") String macAddress, 
										 RedirectAttributes ra) {
                Device d = deviceService.findByMacAddress(macAddress);

                d.setOperator(null);
                d.setLatitude(null);
                d.setLongitude(null);
                d.setActivated(false);
                deviceService.save(d);
                ra.addFlashAttribute("successMessage", "Operator removed.");
                return "redirect:/admin/group/"+projectId;
        }
	
	public void loadDeviceDTO(List<Device> devices, Model model) {
		List<DeviceDTO> deviceDTOs = devices.stream().map(d -> new DeviceDTO(d.getName(), d.getMacAddress(),
				d.getEmailOwner(), d.getDevEui(), d.getLongitude(), d.getLatitude(), d.getTod().getName(), d.getVisibleUsername()))
				.collect(Collectors.toList());
		Collections.sort(deviceDTOs, new Comparator<DeviceDTO>() {

			@Override
			public int compare(DeviceDTO d1, DeviceDTO d2) {

				return d1.getEmailOwner().compareTo(d2.getEmailOwner());
			}

		});
		model.addAttribute("devices", deviceDTOs);
	}
}