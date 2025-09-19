package it.sensorplatform.controller;

import static it.sensorplatform.model.Credentials.FIRE_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.LTRAD_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.VOLCANO_ADMIN_ROLE;

import it.sensorplatform.dto.DeviceDTO;
import it.sensorplatform.model.Admin;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Project;
import it.sensorplatform.model.Superadmin;
import it.sensorplatform.model.User;
import it.sensorplatform.service.AdminService;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.DeviceService;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.service.SuperadminService;
import it.sensorplatform.util.MacAddressUtils;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.util.StringUtils;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class DeviceController {

        private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

        @Autowired
        private DeviceService deviceService;

	@Autowired
	private ProjectService projectService;

        @Autowired
        private CredentialsService credentialsService;

        @Autowired
        private AdminService adminService;

        @Autowired
        private SuperadminService superadminService;

	@GetMapping("/superadmin/manageProjectDevices/{projectId}")
	public String manageProjectDevices(@PathVariable("projectId") Long projectId,
			@RequestParam(value = "deviceQuery", required = false) String deviceQuery, Model model) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
		model.addAttribute("user", credentials);
                Project project = projectService.getProjectById(projectId);
                Superadmin superadmin = superadminService.findByCredentials(credentials);
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
                model.addAttribute("adminEmails",
                                superadmin != null ? superadmin.getAdminEmails() : Collections.emptyList());
                model.addAttribute("project", project);
                model.addAttribute("superadmin", superadmin);
                return "superadmin/manageProjectDevices.html";
        }

        @PostMapping("/superadmin/assignEmailOwner/{projectId}")
        public String assignEmailOwner(@PathVariable("projectId") Long projectId,
                        @RequestParam(value = "selectedDeviceIds", required = false) List<Long> selectedDeviceIds,
                        @RequestParam(value = "existingEmail", required = false) String existingEmail,
                        @RequestParam(value = "newEmail", required = false) String newEmail,
                        RedirectAttributes redirectAttributes) {

                if (selectedDeviceIds == null || selectedDeviceIds.isEmpty()) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                        "Select at least one device without an owner.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }

                final String emailToAssign =
                                StringUtils.hasText(newEmail) ? newEmail.trim()
                                                : StringUtils.hasText(existingEmail) ? existingEmail.trim() : null;

                if (!StringUtils.hasText(emailToAssign)) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                        "Provide an email address to assign to the selected devices.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }

                List<Device> devicesToUpdate = selectedDeviceIds.stream().distinct().map(deviceService::findById)
                                .collect(Collectors.toList());

                if (devicesToUpdate.isEmpty()) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                        "Unable to locate the selected devices.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }

                devicesToUpdate.forEach(device -> {
                        device.setEmailOwner(emailToAssign);
                        deviceService.save(device);
                });

                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
                Superadmin superadmin = superadminService.findByCredentials(credentials);
                if (superadmin != null) {
                        superadmin.addAdminEmail(emailToAssign);
                        superadminService.save(superadmin);
                }

                redirectAttributes.addFlashAttribute("successMessage",
                                devicesToUpdate.size() == 1
                                                ? "Email owner assigned to 1 device."
                                                : "Email owner assigned to " + devicesToUpdate.size() + " devices.");

                return "redirect:/superadmin/manageProjectDevices/" + projectId;
        }

        @PostMapping("/superadmin/assignGsheet/{projectId}")
        public String assignGsheet(@PathVariable("projectId") Long projectId,
                        @RequestParam(value = "selectedDeviceIds", required = false) List<Long> selectedDeviceIds,
                        @RequestParam(value = "gsheetLink", required = false) String gsheetLink,
                        RedirectAttributes redirectAttributes) {

                if (selectedDeviceIds == null || selectedDeviceIds.isEmpty()) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                        "Select at least one device to assign a Google Sheet link.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }

                if (!StringUtils.hasText(gsheetLink)) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                        "Provide a Google Sheet link to assign to the selected devices.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }

                Set<Long> deviceIdsToAssign = selectedDeviceIds.stream().filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                if (deviceIdsToAssign.isEmpty()) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                        "Select at least one device to assign a Google Sheet link.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }

                Set<Device> projectDevices = deviceService.findAllByProjectId(projectId);
                if (projectDevices == null) {
                        projectDevices = Collections.emptySet();
                }

                List<Device> devicesToUpdate = projectDevices.stream()
                                .filter(device -> deviceIdsToAssign.contains(device.getId()))
                                .collect(Collectors.toList());

                if (devicesToUpdate.isEmpty()) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                        "Unable to locate the selected devices.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }

                String trimmedLink = gsheetLink.trim();
                devicesToUpdate.forEach(device -> {
                        device.setGsheet(trimmedLink);
                        deviceService.save(device);
                });

                logger.info("Assigned Google Sheet link to {} device(s) for project {}", devicesToUpdate.size(), projectId);

                redirectAttributes.addFlashAttribute("successMessage",
                                devicesToUpdate.size() == 1
                                                ? "Google Sheet link assigned to 1 device."
                                                : "Google Sheet link assigned to " + devicesToUpdate.size() + " devices.");

                return "redirect:/superadmin/manageProjectDevices/" + projectId;
        }

        @GetMapping("/superadmin/formNewDevice/{projectId}")
        public String formNewDevice(@PathVariable("projectId") Long projectId, Model model) {
                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
                model.addAttribute("user", credentials);
                Project project = projectService.getProjectById(projectId);
                model.addAttribute("project", project);
                return "superadmin/deviceNotifications.html";
        }

        @GetMapping("/superadmin/formUpdateDevice/{projectId}/{macAddress}")
        public String formUpdateDevice(@PathVariable("projectId") Long projectId,
                        @PathVariable("macAddress") String deviceKey, Model model) {
                String normalizedKey = MacAddressUtils.normalize(deviceKey);
                if (normalizedKey == null || normalizedKey.isBlank()) {
                        return "error";
                }
                Project project = projectService.getProjectById(projectId);
                Optional<Device> deviceOpt = deviceService.findOptionalByDeviceKey(normalizedKey);
                if (deviceOpt.isEmpty()) {
                        return "error";
                }
                Device device = deviceOpt.get();
                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
                model.addAttribute("user", credentials);
                model.addAttribute("project", project);
                model.addAttribute("device", device);
                return "superadmin/formUpdateDevice.html";
        }

        @PostMapping("/superadmin/updateDevice/{projectId}/{macAddress}")
        public String adminUpdateDevice(@PathVariable("projectId") Long projectId,
                        @PathVariable("macAddress") String deviceKey, @RequestParam String name,
                        @RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude,
                        RedirectAttributes redirectAttributes) {

                if (name == null || name.trim().isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Device name is required.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }

                String normalizedKey = MacAddressUtils.normalize(deviceKey);
                if (normalizedKey == null || normalizedKey.isBlank()) {
                        redirectAttributes.addFlashAttribute("error", "Device not found.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }

                Optional<Device> deviceOpt = deviceService.findOptionalByDeviceKey(normalizedKey);
                if (deviceOpt.isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Device not found.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }

                Device device = deviceOpt.get();
                // Aggiorna solo i campi modificabili
                device.setName(name);
                if (latitude != null && longitude != null) {
                        device.setLatitude(latitude);
                        device.setLongitude(longitude);
                        device.setStatus("activated");
                } else {
                        device.setLatitude(null);
                        device.setLongitude(null);
                        device.setStatus("deactivated");
                }

                // Salvataggio
                deviceService.save(device);

                redirectAttributes.addFlashAttribute("success", "Device updated successfully.");
                return "redirect:/superadmin/manageProjectDevices/" + projectId;
        }

        @PostMapping("/superadmin/deleteDevice/{projectId}/{macAddress}")
        public String deleteDevice(@PathVariable("projectId") Long projectId, @PathVariable("macAddress") String deviceKey,
                        RedirectAttributes redirectAttributes) {
                String normalizedKey = MacAddressUtils.normalize(deviceKey);
                if (normalizedKey == null || normalizedKey.isBlank()) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Device not found.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }
                Optional<Device> deviceOpt = deviceService.findOptionalByDeviceKey(normalizedKey);
                if (deviceOpt.isEmpty()) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Device not found.");
                        return "redirect:/superadmin/manageProjectDevices/" + projectId;
                }
                deviceService.delete(deviceOpt.get());
                redirectAttributes.addFlashAttribute("successMessage", "Dispositivo eliminato.");
                return "redirect:/superadmin/manageProjectDevices/" + projectId;
        }

        @GetMapping("/device/{projectId}/{macAddress}")
        public String aboutDevice(@PathVariable("projectId") Long projectId, @PathVariable("macAddress") String deviceKey,
                        Model model) {
                String normalizedKey = MacAddressUtils.normalize(deviceKey);
                Project project = projectService.getProjectById(projectId);
                if (normalizedKey == null || normalizedKey.isBlank()) {
                        return "error";
                }
                Optional<Device> deviceOpt = deviceService.findOptionalByDeviceKey(normalizedKey);
                if (deviceOpt.isEmpty()) {
                        return "error";
                }
                Device device = deviceOpt.get();
                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
                model.addAttribute("user", credentials);
                model.addAttribute("project", project);
                model.addAttribute("device", device);
		return "updateDevice";
	}

        @PostMapping("/updateDevice/{projectId}/{macAddress}")
        public String updateDevice(@PathVariable Long projectId, @PathVariable String macAddress, @RequestParam String name,
                        @RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude,
                        RedirectAttributes redirectAttributes) {

                String normalizedKey = MacAddressUtils.normalize(macAddress);

                if (name == null || name.trim().isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Device name is required.");
                        return "redirect:/device/" + projectId + "/" + normalizedKey;
                }

                if (normalizedKey == null || normalizedKey.isBlank()) {
                        redirectAttributes.addFlashAttribute("error", "Device not found.");
                        return "redirect:/device/" + projectId + "/" + normalizedKey;
                }

                Optional<Device> deviceOpt = deviceService.findOptionalByDeviceKey(normalizedKey);
                if (deviceOpt.isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Device not found.");
                        return "redirect:/device/" + projectId + "/" + normalizedKey;
                }

                Device device = deviceOpt.get();
                // Aggiorna solo i campi modificabili
                device.setName(name);
                if (latitude != null && longitude != null) {
                        device.setLatitude(latitude);
                        device.setLongitude(longitude);
                        device.setStatus("activated");
                } else {
                        device.setLatitude(null);
                        device.setLongitude(null);
                        device.setStatus("deactivated");
                }

                // Salvataggio
                deviceService.save(device);

                redirectAttributes.addFlashAttribute("success", "Device updated successfully.");
                return "redirect:/device/" + projectId + "/" + normalizedKey;
        }

	@GetMapping("/admin/formRegisterOperator/{projectId}")
	public String formRegisterOperator(@PathVariable Long projectId, Model model) {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
		model.addAttribute("user", credentials);
		// model.addAttribute("user", new User());
		model.addAttribute("credentials", new Credentials());
		model.addAttribute("projectId", projectId);

            return "admin/formRegisterOperator";

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
                            return "admin/formRegisterOperator";
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
				
            return "admin/formRegisterOperator";

	}

        @PostMapping("/admin/selectOperator/{macAddress}/{opId}/{projectId}")
        public String assignOperatorToDevice(@PathVariable ("projectId") Long projectId, @PathVariable ("macAddress") String macAddress,
                                                                                @PathVariable("opId") Long opId, RedirectAttributes ra) {
                String normalizedKey = MacAddressUtils.normalize(macAddress);
                if (normalizedKey == null || normalizedKey.isBlank()) {
                        ra.addFlashAttribute("errorMessage", "Device not found.");
                        return "redirect:/admin/group/"+projectId;
                }
                Optional<Device> deviceOpt = deviceService.findOptionalByDeviceKey(normalizedKey);
                if (deviceOpt.isEmpty()) {
                        ra.addFlashAttribute("errorMessage", "Device not found.");
                        return "redirect:/admin/group/"+projectId;
                }
                Device d = deviceOpt.get();
                Credentials operator = credentialsService.findById(opId);

                d.setOperator(operator);
                deviceService.save(d);

                return "redirect:/admin/group/"+projectId;
        }

        @PostMapping("/admin/removeOperator/{macAddress}/{projectId}")
        public String removeOperatorfromDevice(@PathVariable ("projectId") Long projectId, @PathVariable ("macAddress") String macAddress,
                                                                                 RedirectAttributes ra) {
                String normalizedKey = MacAddressUtils.normalize(macAddress);
                if (normalizedKey == null || normalizedKey.isBlank()) {
                        ra.addFlashAttribute("errorMessage", "Device not found.");
                        return "redirect:/admin/group/"+projectId;
                }
                Optional<Device> deviceOpt = deviceService.findOptionalByDeviceKey(normalizedKey);
                if (deviceOpt.isEmpty()) {
                        ra.addFlashAttribute("errorMessage", "Device not found.");
                        return "redirect:/admin/group/"+projectId;
                }
                Device d = deviceOpt.get();

                d.setOperator(null);
                d.setLatitude(null);
                d.setLongitude(null);
                d.setStatus("deactivated");
                deviceService.save(d);
                ra.addFlashAttribute("successMessage", "Operator removed.");
                return "redirect:/admin/group/"+projectId;
        }

        @GetMapping("/admin/device-dashboard/{projectId}/{macAddress}")
        public String viewDeviceDashboard(@PathVariable("projectId") Long projectId,
                                          @PathVariable("macAddress") String macAddress,
                                          Model model) {
                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
                model.addAttribute("user", credentials);

                if (credentials.getAdmin() == null) {
                        return "error";
                }

                Project project = projectService.getProjectById(projectId);
                if (project == null || !Objects.equals(project.getId(), credentials.getProjectId())) {
                        return "error";
                }

                if (!isAdminRoleForProject(credentials.getRole(), project.getName())) {
                        return "error";
                }

                String normalizedMac = MacAddressUtils.normalize(macAddress);
                Optional<Device> deviceOpt = deviceService.findOptionalByDeviceKey(normalizedMac);
                if (deviceOpt.isEmpty()) {
                        return "error";
                }

                Device device = deviceOpt.get();
                if (device.getProject() == null || !Objects.equals(device.getProject().getId(), projectId)) {
                        return "error";
                }

                model.addAttribute("project", project);
                model.addAttribute("device", device);
                String projectKey = project.getName() != null ? project.getName().toLowerCase(Locale.ROOT) : "default";
                model.addAttribute("projectKey", projectKey);
                model.addAttribute("specs", device.getTod() != null && device.getTod().getSpecs() != null
                                ? device.getTod().getSpecs() : List.of());

                return "admin/deviceDashboard";
        }

        private boolean isAdminRoleForProject(String role, String projectName) {
                if (role == null || projectName == null) {
                        return false;
                }
                String normalizedProject = projectName.toUpperCase(Locale.ROOT);
                return switch (normalizedProject) {
                        case "LTRAD" -> LTRAD_ADMIN_ROLE.equals(role);
                        case "FIRE" -> FIRE_ADMIN_ROLE.equals(role);
                        case "VOLCANO" -> VOLCANO_ADMIN_ROLE.equals(role);
                        default -> false;
                };
        }

        public void loadDeviceDTO(List<Device> devices, Model model) {
                List<DeviceDTO> deviceDTOs = devices.stream().map(d -> new DeviceDTO(d.getId(), d.getName(),
                                d.getMacAddress(), d.getEmailOwner(), d.getDevEui(), d.getLongitude(), d.getLatitude(),
                                d.getTod() != null ? d.getTod().getName() : null, d.getVisibleUsername(), d.getStatus(),
                                d.getGsheet()))
                                .collect(Collectors.toList());
                Comparator<DeviceDTO> cmp = Comparator.comparing(DeviceDTO::getEmailOwner,
                                Comparator.nullsFirst(String::compareTo));
                Collections.sort(deviceDTOs, cmp);
                model.addAttribute("devices", deviceDTOs);
                List<DeviceDTO> devicesWithoutOwner = deviceDTOs.stream().filter(DeviceDTO::isEmailOwnerMissing)
                                .collect(Collectors.toList());
                model.addAttribute("devicesWithoutOwner", devicesWithoutOwner);
        }
}
