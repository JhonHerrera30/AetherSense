package it.sensorplatform.controller;

import java.util.Collections;
import java.util.Comparator;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Spec;
import it.sensorplatform.model.TypeOfDevice;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.service.SpecService;
import it.sensorplatform.service.TypeOfDeviceService;
import jakarta.validation.Valid;

@Controller
public class TypeOfDeviceController {

	@Autowired
	private TypeOfDeviceService todService;
	
	@Autowired
	private ProjectService projectService;
	
	@Autowired
	private SpecService specService;
	
	@Autowired
	private CredentialsService credentialsService;
	
	
	@GetMapping("/admin/formNewTod/{projectId}")
	public String formNewTod(@PathVariable("projectId") Long projectId, Model model) {
		UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
	    model.addAttribute("user", credentials);
		model.addAttribute("project", projectService.getProjectById(projectId));
		model.addAttribute("tod", new TypeOfDevice());
		List<Spec> allSpecs=specService.findAll();
		Collections.sort(allSpecs, new Comparator<Spec>(){

			@Override
			public int compare(Spec s1, Spec s2) {
				return s1.getMeasurement().compareTo(s2.getMeasurement());
			}
			
		});
		model.addAttribute("specs", allSpecs);
		return "admin/formNewTod";
	}
	
	@PostMapping("/admin/newTod/{projectId}")
	public String saveNewTod(@PathVariable("projectId") Long projectId, @Valid @ModelAttribute("tod") TypeOfDevice tod, BindingResult bindingResult, 
			@RequestParam(value="specs", required = false) List<Long> specsId, 
			RedirectAttributes redirectAttributes, Model model) {
		UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
	    model.addAttribute("user", credentials);
		
		if(bindingResult.hasErrors()) {
			if(specsId==null || specsId.isEmpty()) {
				model.addAttribute("noSpecsSelected", "Please select at least one specification");

			}
			model.addAttribute("project", projectService.getProjectById(projectId));
			model.addAttribute("tod", tod);
			List<Spec> allSpecs=specService.findAll();
			Collections.sort(allSpecs, new Comparator<Spec>(){

				@Override
				public int compare(Spec s1, Spec s2) {
					return s1.getMeasurement().compareTo(s2.getMeasurement());
				}
				
			});
			model.addAttribute("specs", allSpecs);
			
			return "admin/formNewTod";
		}
		
		projectService.getProjectById(projectId).getTods().add(tod);
		List<Spec> selectedSpecs = specService.findAllById(specsId);
		tod.setSpecs(selectedSpecs);
		todService.save(tod);
		redirectAttributes.addFlashAttribute("success", "New type of device successfully added");
		return "redirect:/admin/formNewDevice/" + projectId;
	}
}
