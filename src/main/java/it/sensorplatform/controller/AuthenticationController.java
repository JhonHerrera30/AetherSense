package it.sensorplatform.controller;


import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
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

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Project;
import it.sensorplatform.model.User;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.DeviceService;
import it.sensorplatform.service.ProjectService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import static it.sensorplatform.model.Credentials.SUPERADMIN_ROLE;

import static it.sensorplatform.model.Credentials.LTRAD_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.FIRE_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.VOLCANO_ADMIN_ROLE;

import static it.sensorplatform.model.Credentials.LTRAD_OPERATOR_ROLE;
import static it.sensorplatform.model.Credentials.FIRE_OPERATOR_ROLE;
import static it.sensorplatform.model.Credentials.VOLCANO_OPERATOR_ROLE;
@Controller
public class AuthenticationController {

	@Autowired
	private CredentialsService credentialsService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private DeviceService deviceService;

	@GetMapping(value = "/register") 
	public String showRegisterForm (@RequestParam(value = "projectId", required = false) Long projectId, Model model) {
		model.addAttribute("user", new User());
		model.addAttribute("credentials", new Credentials());
		model.addAttribute("projectId", projectId);
		List<Project> projects = (List<Project>) projectService.getAllProjects();
		model.addAttribute("projects", projects);
		return "formRegisterUser";
	}


	@GetMapping("/login") 
	public String login(@RequestParam(value = "projectId", required = false) Long projectId,
			@RequestParam(value = "error", required = false) String error,
			Model model) {
		model.addAttribute("projectId", projectId);
		model.addAttribute("error", error != null);
		List<Project> projects = (List<Project>) projectService.getAllProjects();
		model.addAttribute("projects", projects);
		return "formLogin";
	}


	@GetMapping(value = "/") 
	public String index(Model model) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		model.addAttribute("ltrad", this.projectService.getProjectByName("LTRAD"));
		model.addAttribute("fire", this.projectService.getProjectByName("FIRE"));
		model.addAttribute("volcano", this.projectService.getProjectByName("VOLCANO"));
		if (authentication instanceof AnonymousAuthenticationToken) {
			return "home.html";
		}
		else {		
			UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
			model.addAttribute("user", credentials);
			if (credentials.getRole().equals(SUPERADMIN_ROLE)) {
				return "superadmin/superadminHome.html";
			}
			
			else{
				model.addAttribute("user", credentials);
				return "home.html";
			}
		}
	}
	
	
	@GetMapping("/success")
	public String defaultAfterLogin(@RequestParam(value = "projectId", required = false) Long projectId, Model model) {
		UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
	    model.addAttribute("user", credentials);
		if (credentials.getRole().equals(SUPERADMIN_ROLE)) {
			model.addAttribute("ltrad", this.projectService.getProjectByName("LTRAD"));
			model.addAttribute("fire", this.projectService.getProjectByName("FIRE"));
			model.addAttribute("volcano", this.projectService.getProjectByName("VOLCANO"));
			return "superadmin/superadminHome";
		}
		else {
		Project project = projectService.getProjectById(projectId);
		model.addAttribute("project", project);
		
	    // mappa admin (groups.html)
	    if (credentials.getRole().equals(LTRAD_ADMIN_ROLE) || credentials.getRole().equals(FIRE_ADMIN_ROLE) || credentials.getRole().equals(VOLCANO_ADMIN_ROLE)) {
	        return "redirect:/admin/group/" + projectId;
	    }

	    // mappa operator (operator.html)
	    if (credentials.getRole().contains("OPERATOR")) {
	        return "redirect:/operator/" + projectId;
	    }
		
		}
		
		return "error";
	}
	

	@PostMapping(value = { "/register" })
	public String registerUser(@Valid @ModelAttribute("user") User user,
			BindingResult userBindingResult, @Valid
			@ModelAttribute("credentials") Credentials credentials,
			BindingResult credentialsBindingResult,
			@RequestParam("confirmPassword") String confirmPassword,
			Model model) {	 

		boolean error = false;
		Long projectId = credentials.getProjectId();
		Project project = projectService.getProjectById(projectId);
		String email = credentials.getEmail();
		Set<Device> owned  = this.deviceService.findAllByEmailAndProjectId(email,projectId);
		String username = credentials.getUsername();
		String projectName = project.getName();
		credentials.setVisibleUsername(username);
		username = username + "|" + projectName;
		// se user e credential hanno entrambi contenuti validi, memorizza User e the Credentials nel DB
		if(!userBindingResult.hasErrors() && ! credentialsBindingResult.hasErrors()) {
			if (!credentials.getPassword().equals(confirmPassword)) {
				error = true;
				model.addAttribute("passwordMismatchError", "The passwords do not match.");
			}
			if(owned!=null&&owned.isEmpty()) {
				error = true;
				model.addAttribute("noDevicesYet", "No devices owned in this project");
			}
			if(credentialsService.existsByUsername(username)) {
				error = true;
				model.addAttribute("usernameAlreadyInUse", "Username already in use for this project");
			}
			if(credentialsService.existsByEmailAndProjectId(email,projectId)) {
				error = true;
				model.addAttribute("emailAlreadyInUse", "Email already in use for this project");
			}
			if(error) {
				model.addAttribute("projectId", projectId);
				List<Project> projects = (List<Project>) projectService.getAllProjects();
				model.addAttribute("projects", projects);
				return "formRegisterUser";
			}
			credentials.setUsername(username);
			credentials.setUser(user);
			if (project.getName().equals("LTRAD")) {
				credentials.setRole(Credentials.LTRAD_ADMIN_ROLE);
			}
			if (project.getName().equals("FIRE")) {
				credentials.setRole(Credentials.FIRE_ADMIN_ROLE);
			}
			if (project.getName().equals("VOLCANO")) {
				credentials.setRole(Credentials.VOLCANO_ADMIN_ROLE);
			}
			credentialsService.saveCredentials(credentials);
			model.addAttribute("user", user);
			model.addAttribute("projectId", projectId);

			return "registrationSuccessful";
		}
		model.addAttribute("projectId", projectId);
		List<Project> projects = (List<Project>) projectService.getAllProjects();
		model.addAttribute("projects", projects);
		return "formRegisterUser";
	}
	
	
	@GetMapping("/access")
	public String accessProject(@RequestParam("projectId") Long projectId, HttpSession session, Model model) {
	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

	    if (authentication instanceof AnonymousAuthenticationToken) {
	        // utente non autenticato → reindirizza al login con projectId
	        return "redirect:/login?projectId=" + projectId;
	    }

	    // utente autenticato → controlla se ha accesso a quel progetto
	    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
	    Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());

	    Project project = projectService.getProjectById(projectId);
	    session.setAttribute("projectId", projectId); // salva projectId in sessione
	    model.addAttribute("project", project);
	    model.addAttribute("user", credentials);

	    // se admin → view admin con lista progetti
	    if (credentials.getRole().equals(SUPERADMIN_ROLE)) {
	        model.addAttribute("ltrad", projectService.getProjectByName("LTRAD"));
	        model.addAttribute("fire", projectService.getProjectByName("FIRE"));
	        model.addAttribute("volcano", projectService.getProjectByName("VOLCANO"));
	        return "superadmin/superadminHome";
	    }

		if(project.getName().equals("LTRAD") && credentials.getRole().equals(LTRAD_ADMIN_ROLE)) {
			return "redirect:/admin/group/" +projectId;
		}
		if(project.getName().equals("FIRE") && credentials.getRole().equals(FIRE_ADMIN_ROLE)) {
			return "redirect:/admin/group/" +projectId;		}
		if(project.getName().equals("VOLCANO") && credentials.getRole().equals(VOLCANO_ADMIN_ROLE)) {
			return "redirect:/admin/group/" +projectId;		}
//		if(project.getName()!=null && credentials.getRole().contains("_ADMIN_ROLE"))
//			return "redirect:/admin/group/"+projectId;
		if(credentials.getRole().contains("OPERATOR")) {
			return "redirect:/operator/"+ projectId;
		}

	    // fallback se ruolo non corrisponde
	    return "redirect:/login?projectId=" + projectId;
	}	
	
	@GetMapping("/project/{projectId}/info")
	public String projectInfo(@PathVariable Long projectId, Model model) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

	    if (!(authentication instanceof AnonymousAuthenticationToken)) {
	    	// utente autenticato → controlla se ha accesso a quel progetto
	         UserDetails userDetails = (UserDetails) authentication.getPrincipal();
	         Credentials credentials = credentialsService.getCredentials(userDetails.getUsername());
	         model.addAttribute("user", credentials);
	    }
	    Project project = projectService.getProjectById(projectId);
	    if (project == null) {
	        return "error";
	    }
	    model.addAttribute("project", project);
	    
	    switch (project.getName()) {
	        case "LTRAD":
	            return "/project/ltradHome";
	        case "FIRE":
	            return "/project/fireHome";
	        case "VOLCANO":
	            return "/project/volcanoHome";
	        default:
	            return "error";
	    }
	}

}