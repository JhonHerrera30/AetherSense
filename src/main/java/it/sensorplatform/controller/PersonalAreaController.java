package it.sensorplatform.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.sensorplatform.dto.CredentialsUpdateForm;
import it.sensorplatform.dto.PersonalInfoForm;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Project;
import it.sensorplatform.model.User;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.service.UserService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/personal-area")
public class PersonalAreaController {

    private static final String PERSONAL_AREA_VIEW = "personalArea";

    @Autowired
    private CredentialsService credentialsService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public String showPersonalArea(@RequestParam(value = "projectId", required = false) Long projectId,
            Model model) {
        Credentials credentials = getCurrentCredentials();
        if (credentials == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", credentials);

        if (!model.containsAttribute("personalInfoForm")) {
            model.addAttribute("personalInfoForm", buildPersonalInfoForm(credentials.getUser()));
        }
        if (!model.containsAttribute("credentialsUpdateForm")) {
            CredentialsUpdateForm credentialsForm = new CredentialsUpdateForm();
            credentialsForm.setUsername(credentials.getVisibleUsername());
            model.addAttribute("credentialsUpdateForm", credentialsForm);
        }

        boolean isSuperadmin = Credentials.SUPERADMIN_ROLE.equals(credentials.getRole());
        model.addAttribute("isSuperadmin", isSuperadmin);

        List<Project> projects = collectProjects();
        model.addAttribute("projects", projects);

        Long effectiveProjectId = determineProjectId(credentials, projectId, isSuperadmin);
        Project activeProject = resolveProject(effectiveProjectId);

        if (isSuperadmin) {
            model.addAttribute("projectUsers", credentialsService.getProjectUsers(effectiveProjectId));
        } else {
            model.addAttribute("projectUsers", Collections.emptyList());
        }

        model.addAttribute("selectedProjectId", effectiveProjectId);
        model.addAttribute("activeProject", activeProject);
        model.addAttribute("projectKey", determineProjectKey(activeProject));

        return PERSONAL_AREA_VIEW;
    }

    @PostMapping("/update-info")
    public String updatePersonalInformation(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @Valid @ModelAttribute("personalInfoForm") PersonalInfoForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        Credentials credentials = getCurrentCredentials();
        if (credentials == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "personalInfoForm", bindingResult);
            redirectAttributes.addFlashAttribute("personalInfoForm", form);
            redirectAttributes.addFlashAttribute("personalInfoErrorMessage", "Verifica i dati inseriti e riprova.");
            if (projectId != null) {
                redirectAttributes.addAttribute("projectId", projectId);
            }
            return "redirect:/personal-area";
        }

        userService.updatePersonalInfo(credentials, form);
        credentialsService.updateCredentials(credentials, null);

        redirectAttributes.addFlashAttribute("personalInfoSuccessMessage", "Dati anagrafici aggiornati con successo.");
        if (projectId != null) {
            redirectAttributes.addAttribute("projectId", projectId);
        }
        return "redirect:/personal-area";
    }

    @PostMapping("/update-credentials")
    public String updateCredentials(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @Valid @ModelAttribute("credentialsUpdateForm") CredentialsUpdateForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        Credentials credentials = getCurrentCredentials();
        if (credentials == null) {
            return "redirect:/login";
        }

        String normalizedUsername = form.getUsername() != null ? form.getUsername().trim() : "";
        if (!normalizedUsername.equals(form.getUsername())) {
            form.setUsername(normalizedUsername);
        }

        String suffix = resolveUsernameSuffix(credentials);
        String rebuiltUsername = buildUsername(normalizedUsername, suffix);

        if (!rebuiltUsername.equals(credentials.getUsername())
                && credentialsService.existsByUsername(rebuiltUsername)) {
            bindingResult.rejectValue("username", "username.alreadyInUse", "Username già in uso");
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "credentialsUpdateForm",
                    bindingResult);
            CredentialsUpdateForm safeForm = new CredentialsUpdateForm();
            safeForm.setUsername(form.getUsername());
            redirectAttributes.addFlashAttribute("credentialsUpdateForm", safeForm);
            redirectAttributes.addFlashAttribute("credentialsErrorMessage", "Correggi gli errori segnalati e riprova.");
            if (projectId != null) {
                redirectAttributes.addAttribute("projectId", projectId);
            }
            return "redirect:/personal-area";
        }

        credentials.setVisibleUsername(normalizedUsername);
        credentials.setUsername(rebuiltUsername);
        String password = form.getPassword();
        if (!StringUtils.hasText(password)) {
            password = null;
        }
        credentialsService.updateCredentials(credentials, password);

        redirectAttributes.addFlashAttribute("credentialsSuccessMessage", "Credenziali aggiornate con successo.");
        if (projectId != null) {
            redirectAttributes.addAttribute("projectId", projectId);
        }
        return "redirect:/personal-area";
    }

    private Credentials getCurrentCredentials() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return credentialsService.getCredentials(userDetails.getUsername());
        }
        if (principal instanceof String username) {
            return credentialsService.getCredentials(username);
        }
        return null;
    }

    private PersonalInfoForm buildPersonalInfoForm(User user) {
        PersonalInfoForm form = new PersonalInfoForm();
        if (user != null) {
            form.setName(user.getName());
            form.setSurname(user.getSurname());
            form.setDateOfBirth(user.getDateOfBirth());
            form.setPhoneNumber(user.getPhoneNumber());
        }
        return form;
    }

    private List<Project> collectProjects() {
        List<Project> projects = new ArrayList<>();
        projectService.getAllProjects().forEach(projects::add);
        return projects;
    }

    private Long determineProjectId(Credentials credentials, Long requestedProjectId, boolean isSuperadmin) {
        if (isSuperadmin) {
            if (requestedProjectId != null) {
                return requestedProjectId;
            }
        }
        return credentials.getProjectId();
    }

    private Project resolveProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        try {
            return projectService.getProjectById(projectId);
        } catch (NoSuchElementException ex) {
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String determineProjectKey(Project project) {
        if (project != null && project.getName() != null) {
            return project.getName().toLowerCase(Locale.ROOT);
        }
        return "default";
    }

    private String resolveUsernameSuffix(Credentials credentials) {
        if (Credentials.SUPERADMIN_ROLE.equals(credentials.getRole())) {
            return Credentials.SUPERADMIN_ROLE;
        }
        Project project = resolveProject(credentials.getProjectId());
        if (project != null && StringUtils.hasText(project.getName())) {
            return project.getName();
        }
        String username = credentials.getUsername();
        if (username != null) {
            int separatorIndex = username.indexOf('|');
            if (separatorIndex >= 0 && separatorIndex < username.length() - 1) {
                return username.substring(separatorIndex + 1);
            }
        }
        return null;
    }

    private String buildUsername(String visibleUsername, String suffix) {
        if (!StringUtils.hasText(suffix)) {
            return visibleUsername;
        }
        return visibleUsername + "|" + suffix;
    }
}
