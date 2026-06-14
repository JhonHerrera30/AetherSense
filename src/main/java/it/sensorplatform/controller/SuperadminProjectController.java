package it.sensorplatform.controller;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Project;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.repository.ProjectRepository;
import it.sensorplatform.util.ApiKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/superadmin")
public class SuperadminProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CredentialsService credentialsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public record NewProjectResult(
            String projectName,
            String adminUsername,
            String adminPassword,
            String apiKey) {
    }

    @PostMapping("/create-project")
    public String createProject(
            @RequestParam String projectName,
            @RequestParam String adminEmail,
            @RequestParam String adminUsername,
            RedirectAttributes redirectAttributes) {

        // Verifica che il nome progetto non esista già
        if (projectService.getProjectByName(projectName.toUpperCase()) != null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Project '" + projectName + "' already exists.");
            return "redirect:/success";
        }

        // Crea il progetto
        Project project = new Project();
        project.setName(projectName.toUpperCase());
        project = projectService.save(project);

        // Genera API key
        String rawApiKey = ApiKeyUtil.generateRawKey();
        String apiKeyHash = ApiKeyUtil.hashKey(rawApiKey);
        project.setApiKeyHash(apiKeyHash);
        projectService.save(project);

        // Genera password admin casuale
        String rawPassword = ApiKeyUtil.generateRawKey().substring(0, 12);

        // Crea credenziali admin
        String fullUsername = adminUsername + "|" + projectName.toUpperCase();

        if (credentialsService.existsByUsername(fullUsername)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Username '" + adminUsername + "' already in use.");
            return "redirect:/success";
        }

        Credentials adminCredentials = new Credentials();
        adminCredentials.setUsername(fullUsername);
        adminCredentials.setVisibleUsername(adminUsername);
        adminCredentials.setEmail(adminEmail);
        adminCredentials.setPassword(rawPassword);
        adminCredentials.setRole(Credentials.ADMIN_ROLE);
        adminCredentials.setProjectId(project.getId());
        credentialsService.saveCredentials(adminCredentials);

        // Mostra risultato una sola volta
        NewProjectResult result = new NewProjectResult(
                project.getName(),
                fullUsername,
                rawPassword,
                rawApiKey);

        redirectAttributes.addFlashAttribute("newProjectResult", result);
        redirectAttributes.addFlashAttribute("successMessage",
                "Project '" + project.getName() + "' created successfully!");
        return "redirect:/success";
    }
}