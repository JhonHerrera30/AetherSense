package it.sensorplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.sensorplatform.dto.DeviceDTO;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Group;
import it.sensorplatform.model.Project;
import it.sensorplatform.model.Admin;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.DeviceService;
import it.sensorplatform.service.GroupService;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.service.AdminService;
import it.sensorplatform.util.MacAddressUtils;

import static it.sensorplatform.model.Credentials.SUPERADMIN_ROLE;

import static it.sensorplatform.model.Credentials.LTRAD_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.FIRE_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.VOLCANO_ADMIN_ROLE;

import static it.sensorplatform.model.Credentials.LTRAD_OPERATOR_ROLE;
import static it.sensorplatform.model.Credentials.FIRE_OPERATOR_ROLE;
import static it.sensorplatform.model.Credentials.VOLCANO_OPERATOR_ROLE;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Comparator;

@Controller
public class GroupController {
        @Autowired
        private GroupService groupService;

        @Autowired
        private CredentialsService credentialsService;

        @Autowired
        private ProjectService projectService;

        @Autowired
        private DeviceService deviceService;

        @Autowired
        private AdminService adminService;

        @GetMapping(value = "/admin/group/{id}")
        public String group(@PathVariable("id") Long projectId,
                        @RequestParam(value = "groupName", required = false) String groupName,
                        @RequestParam(value = "deviceInfo", required = false) String deviceInfo, Model model) {

                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
                model.addAttribute("user", credentials);
                if (credentials.getRole().equals(SUPERADMIN_ROLE)) {
                        model.addAttribute("ltrad", this.projectService.getProjectByName("LTRAD"));
                        model.addAttribute("fire", this.projectService.getProjectByName("FIRE"));
                        model.addAttribute("volcano", this.projectService.getProjectByName("VOLCANO"));
                        return "admin/adminHome";
                } else {
                        Project project = this.projectService.getProjectById(projectId);

                        // List<Credentials> operators =
                        // credentialsService.findOperatorsByProject(project);
                        // model.addAttribute("operators", operators);

                        Admin admin = credentials.getAdmin();
                        if (admin != null) {
                                admin = this.adminService.getAdmin(admin.getId());
                                model.addAttribute("authorizedOperators", admin.getAuthorizedOperators());
                        }

                        Set<Group> groups;
                        if (groupName != null && !groupName.isEmpty()) {
                                groups = groupService.findByNameStartingWithIgnoreCaseAndCredentials(groupName,
                                                credentials);
                        } else {
                                groups = groupService.findAllByCredentials(credentials);
                        }
                        Set<Device> devices = deviceService.findAllByEmailAndProjectId(credentials.getEmail(),
                                        projectId);
                        Set<Device> devicesFiltered;

                        if (deviceInfo != null && !deviceInfo.isEmpty()) {
                                devicesFiltered = deviceService.findByNameStartingWithIgnoreCase(deviceInfo);
                                devicesFiltered.addAll(
                                                deviceService.findByMacAddressStartingWithIgnoreCase(deviceInfo));

                                // Mantieni solo gli elementi comuni
                                devices.retainAll(devicesFiltered);
                        } else {
                                devicesFiltered = devices;
                        }
                        this.loadDeviceDTO(devices, model);
                        model.addAttribute("project", project);
                        model.addAttribute("groups", groups);
                        if (project.getName().equals("LTRAD") && (credentials.getRole().equals(LTRAD_ADMIN_ROLE)
                                        || credentials.getRole().equals(LTRAD_OPERATOR_ROLE))) {
                                return "admin/groups";
                        } else if (project.getName().equals("FIRE") && (credentials.getRole().equals(FIRE_ADMIN_ROLE)
                                        || credentials.getRole().equals(FIRE_OPERATOR_ROLE))) {
                                return "admin/groups";
                        } else if (project.getName().equals("VOLCANO")
                                        && (credentials.getRole().equals(VOLCANO_ADMIN_ROLE)
                                                        || credentials.getRole().equals(VOLCANO_OPERATOR_ROLE))) {
                                return "admin/groups";
                        } else if (credentials.getRole().equals(Credentials.ADMIN_ROLE)
                                        && credentials.getProjectId().equals(projectId)) {
                                return "admin/groups";
                        }
                }
                return "error";
        }

        @GetMapping("manageGroups/{projectId}")
        public String manageGroup(@PathVariable("projectId") Long projectId,
                        @RequestParam(value = "groupName", required = false) String groupName, Model model) {
                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
                model.addAttribute("user", credentials);
                Project project = this.projectService.getProjectById(projectId);
                Set<Group> groups;
                if (groupName != null && !groupName.isEmpty()) {
                        groups = groupService.findByNameStartingWithIgnoreCaseAndCredentials(groupName, credentials);
                } else {
                        groups = groupService.findAllByCredentials(credentials);
                }
                model.addAttribute("project", project);
                model.addAttribute("groups", groups);
                return "admin/manageGroups";
        }

        @PostMapping("/group/create/{projectId}")
        public String createGroup(@PathVariable Long projectId, @RequestParam String groupName, Principal principal,
                        RedirectAttributes redirectAttributes) {
                Credentials credentials = credentialsService.getCredentials(principal.getName());
                Project project = projectService.getProjectById(projectId);

                Group group = new Group();
                if (groupService.findGroupByNameAndCredentials(groupName, credentials) != null) {
                        redirectAttributes.addFlashAttribute("errorSameName", "A group with this name already exists.");
                        return "redirect:/manageGroups/" + projectId;
                }
                group.setName(groupName);
                group.setProject(project);
                group.setCredentials(credentials);

                groupService.save(group);
                redirectAttributes.addFlashAttribute("success", "Group created successfully.");
                return "redirect:/manageGroups/" + projectId;
        }

        @PostMapping("/group/delete/{projectId}/{groupId}")
        public String deleteGroup(@PathVariable Long projectId, @PathVariable Long groupId,
                        RedirectAttributes redirectAttributes) {
                Group group = groupService.findGroupById(groupId);

                ///////// *MODIFICHE ESAME*////////////////
                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());

                if (!credentials.getId().equals(group.getCredentials().getId())) {
                        redirectAttributes.addFlashAttribute("deleteError",
                                        "Non sei autorizzato a eliminare questo gruppo.");
                        return "redirect:/manageGroups/" + projectId;
                }

                for (Device d : group.getDevices()) {
                        d.setGroup(null);
                        deviceService.save(d);
                }
                groupService.deleteById(groupId);
                redirectAttributes.addFlashAttribute("success", "Group removed successfully.");
                return "redirect:/manageGroups/" + projectId;
        }

        @PostMapping("/group/{projectId}/{groupId}/removeDevice/{macAddress}")
        public String removeDeviceFromGroup(@PathVariable Long projectId, @PathVariable Long groupId,
                        @PathVariable String macAddress, RedirectAttributes redirectAttributes) {
                String normalizedKey = MacAddressUtils.normalize(macAddress);
                if (normalizedKey == null || normalizedKey.isBlank()) {
                        redirectAttributes.addFlashAttribute("error", "Device not found");
                        return "redirect:/manageGroups/" + projectId;
                }
                Group group = groupService.findGroupById(groupId);
                if (group == null) {
                        redirectAttributes.addFlashAttribute("error", "Group not found");
                        return "redirect:/manageGroups/" + projectId;
                }
                Optional<Device> deviceOpt = deviceService.findOptionalByDeviceKey(normalizedKey);
                if (deviceOpt.isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Device not found");
                        return "redirect:/manageGroups/" + projectId;
                }
                Device device = deviceOpt.get();
                List<Device> devices = group.getDevices();
                devices.remove(device);
                group.setDevices(devices);
                device.setGroup(null);
                groupService.save(group);
                deviceService.save(device);
                redirectAttributes.addFlashAttribute("success", "Device removed successfully.");
                return "redirect:/manageGroups/" + projectId;
        }

        @PostMapping("/group/{groupId}/add-device/{macAddress}")
        public String addDeviceToGroup(@PathVariable Long groupId, @PathVariable("macAddress") String macAddress,
                        Principal principal, RedirectAttributes redirectAttributes) {
                Group group = groupService.findGroupById(groupId);
                if (group == null) {
                        redirectAttributes.addFlashAttribute("error", "Group not found");
                        return "error";
                }
                String normalizedKey = MacAddressUtils.normalize(macAddress);
                if (normalizedKey == null || normalizedKey.isBlank()) {
                        redirectAttributes.addFlashAttribute("error", "Device not found");
                        return "redirect:/manageGroups/" + group.getProject().getId();
                }
                Optional<Device> deviceOpt = deviceService.findOptionalByDeviceKey(normalizedKey);
                if (deviceOpt.isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Device not found");
                } else {
                        Device device = deviceOpt.get();
                        device.setGroup(group);
                        deviceService.save(device);
                        redirectAttributes.addFlashAttribute("success", "Device added successfully.");
                }

                return "redirect:/manageGroups/" + group.getProject().getId();
        }

        /* OPERATOR */
        @GetMapping("/operator/{projectId}")
        public String operatorMap(@PathVariable("projectId") Long projectId,
                        @RequestParam(value = "groupName", required = false) String groupName,
                        @RequestParam(value = "deviceInfo", required = false) String deviceInfo,
                        Model model) {

                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
                model.addAttribute("user", credentials);

                // consenti solo agli operatori dei 3 progetti
                if (!(credentials.getRole().equals(LTRAD_OPERATOR_ROLE) ||
                                credentials.getRole().equals(FIRE_OPERATOR_ROLE) ||
                                credentials.getRole().equals(VOLCANO_OPERATOR_ROLE) ||
                                credentials.getRole().equals(Credentials.OPERATOR_ROLE))) {
                        return "error";
                }
                if (credentials.getProjectId() != null &&
                                !credentials.getProjectId().equals(projectId)) {
                        return "error";
                }

                Project project = this.projectService.getProjectById(projectId);
                if (project == null)
                        return "error";

                // gruppi dell'operatore (servono per i layer/filtri in mappa, se li usi)
                Set<Group> groups = (groupName != null && !groupName.isEmpty())
                                ? groupService.findByNameStartingWithIgnoreCaseAndCredentials(groupName, credentials)
                                : groupService.findAllByCredentials(credentials);

                Credentials operator = credentials; // deve coincidere con ciò che salvi nel Device
                Set<Device> devices = deviceService.findAllByOperator(operator);

                // solo posizionati
                devices.removeIf(d -> d.getLatitude() == null || d.getLongitude() == null);

                // search locale
                if (deviceInfo != null && !deviceInfo.isEmpty()) {
                        String q = deviceInfo.toLowerCase();
                        devices.removeIf(d -> {
                                String deviceName = d.getName();
                                boolean nameMatches = deviceName != null && deviceName.toLowerCase().startsWith(q);
                                String macAddress = d.getMacAddress();
                                boolean macMatches = macAddress != null && macAddress.toLowerCase().startsWith(q);
                                return !(nameMatches || macMatches);
                        });
                }

                // DTO e model
                this.loadDeviceDTO(devices, model);
                model.addAttribute("project", project);
                model.addAttribute("groups", groups);

                // template dedicato agli operatori
                return "operator"; // oppure "operator.html" se il file si chiama così
        }

        @GetMapping("/operator/{projectId}/activations")
        public String operatorActivations(@PathVariable("projectId") Long projectId, Model model) {

                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
                model.addAttribute("user", credentials);

                if (!(credentials.getRole().equals(LTRAD_OPERATOR_ROLE) ||
                                credentials.getRole().equals(FIRE_OPERATOR_ROLE) ||
                                credentials.getRole().equals(VOLCANO_OPERATOR_ROLE))) {
                        return "error";
                }

                Project project = this.projectService.getProjectById(projectId);
                if (project == null) {
                        return "error";
                }

                if (credentials.getProjectId() != null && !credentials.getProjectId().equals(projectId)) {
                        return "error";
                }

                model.addAttribute("project", project);
                return "operator/activationRequests";
        }

        public void loadDeviceDTO(Set<Device> devices, Model model) {
                List<Device> orderedDevices = new ArrayList<>(devices);
                Collections.sort(orderedDevices, new Comparator<Device>() {
                        @Override
                        public int compare(Device d1, Device d2) {
                                String label1 = getDeviceSortLabel(d1);
                                String label2 = getDeviceSortLabel(d2);
                                return label1.compareToIgnoreCase(label2);
                        }
                });
                List<DeviceDTO> deviceDTOs = orderedDevices.stream().map(d -> new DeviceDTO(d.getId(), d.getName(),
                                d.getMacAddress(), d.getEmailOwner(), d.getDevEui(), d.getLongitude(), d.getLatitude(),
                                d.getTod() != null ? d.getTod().getName() : null, d.getVisibleUsername(),
                                d.isActivated(),
                                d.getGsheet()))
                                .collect(Collectors.toList());
                model.addAttribute("devices", deviceDTOs);
        }

        private String getDeviceSortLabel(Device device) {
                if (device == null) {
                        return "";
                }
                String name = device.getName();
                if (name != null && !name.isBlank()) {
                        return name;
                }
                String macAddress = device.getMacAddress();
                if (macAddress != null) {
                        return macAddress;
                }
                return "";
        }

}
