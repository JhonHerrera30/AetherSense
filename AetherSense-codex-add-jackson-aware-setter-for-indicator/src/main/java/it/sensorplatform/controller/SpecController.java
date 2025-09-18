package it.sensorplatform.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Spec;
import it.sensorplatform.model.TypeOfDevice;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.DeviceService;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.service.SpecService;
import jakarta.validation.Valid;

@Controller
public class SpecController {
	@Autowired
	private SpecService specService;
	
	@Autowired
	private ProjectService projectService;
	
	@Autowired
	private CredentialsService credentialsService;
	
	@Autowired
	private DeviceService deviceService;
	
	
	@GetMapping("/admin/formNewSpec/{projectId}")
	public String formNewSpec(@PathVariable("projectId") Long projectId, Model model) {
		UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
	    model.addAttribute("user", credentials);
		model.addAttribute("spec", new Spec());
		model.addAttribute("project", projectService.getProjectById(projectId));
		return "admin/formNewSpec";
	}
	
	@PostMapping("/admin/newSpec/{projectId}")
	public String saveSpec(@Valid @ModelAttribute("spec") Spec spec, BindingResult bindingResult,
			@PathVariable("projectId") Long projectId,
			RedirectAttributes redirectAttributes, Model model) {
		UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
	    model.addAttribute("user", credentials);
		if(bindingResult.hasErrors()) {
			model.addAttribute("spec", spec);
			model.addAttribute("project", projectService.getProjectById(projectId));
			return "admin/formNewSpec";		
		}
		if(specService.existsByFields(spec)) {
			model.addAttribute("spec", spec);
			model.addAttribute("project", projectService.getProjectById(projectId));
			model.addAttribute("specDuplicate", "A specification with the same values for measurement, unit, and component already exists.");
			return "admin/formNewSpec";
		}
		specService.save(spec);
		redirectAttributes.addFlashAttribute("success", "New spec successfully added");
		return "redirect:/admin/formNewTod/" + projectId;
	}

	
	@GetMapping("/admin/deleteSpec/{projectId}/{specId}")
		public String deleteSpec(@PathVariable("projectId") Long projectId, @PathVariable("specId") Long specId, RedirectAttributes redirectAttributes) {
			Spec specToDelete = specService.findById(specId);
			boolean inUse = false;
			List<Device> allDevices = deviceService.findAll();
			for(Device d : allDevices) {
				TypeOfDevice deviceTod = d.getTod();
				if(deviceTod.getSpecs().contains(specToDelete)) {
					inUse=true;
				}
				
			}
			
			if(inUse) {
				redirectAttributes.addFlashAttribute("notDeleted", "This spec belongs to a device, you can't delete it.");
			}
			else {
				specService.deleteSpecById(specId);
				redirectAttributes.addFlashAttribute("deleted", "Spec deleted successfully.");
			}
			
			return "redirect:/admin/formNewTod/" + projectId;
		}
	
}
