package it.sensorplatform.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.sensorplatform.dto.CredentialsUpdateDTO;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Project;
import it.sensorplatform.model.User;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.service.UserService;
import jakarta.validation.Valid;

/**
 * Controller responsible for handling the personal area where authenticated
 * users can review and update their personal information and credentials.
 */
@Controller
public class UserProfileController {

    private static final String PERSONAL_AREA_VIEW = "personalArea";

    @Autowired
    private CredentialsService credentialsService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private MessageSource messageSource;

    @GetMapping("/personal-area")
    public String personalArea(@RequestParam(value = "projectId", required = false) Long projectFilterId, Model model) {
        Credentials credentials = getCurrentCredentials();
        if (credentials == null) {
            return "redirect:/login";
        }

        ensureUserForm(model, credentials);
        ensureCredentialsDto(model, credentials);
        populateCommon(model, credentials, projectFilterId);
        return PERSONAL_AREA_VIEW;
    }

    @PostMapping("/personal-area/update-user")
    public String updateUser(@Valid @ModelAttribute("userForm") User userForm, BindingResult bindingResult,
            @RequestParam(value = "projectId", required = false) Long projectFilterId, RedirectAttributes redirectAttributes,
            Model model) {

        Credentials credentials = getCurrentCredentials();
        if (credentials == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            ensureCredentialsDto(model, credentials);
            populateCommon(model, credentials, projectFilterId);
            return PERSONAL_AREA_VIEW;
        }

        User existingUser = credentials.getUser();
        if (existingUser != null && existingUser.getId() != null) {
            userForm.setId(existingUser.getId());
        }

        User savedUser = userService.saveUser(userForm);
        credentials.setUser(savedUser);
        credentialsService.save(credentials);

        String successMessage = resolveMessage("profile.update.success");
        redirectAttributes.addFlashAttribute("userSuccessMessage", successMessage);
        if (projectFilterId != null) {
            redirectAttributes.addAttribute("projectId", projectFilterId);
        }
        return "redirect:/personal-area";
    }

    @PostMapping("/personal-area/update-credentials")
    public String updateCredentials(@Valid @ModelAttribute("credentialsDto") CredentialsUpdateDTO credentialsDto,
            BindingResult bindingResult, @RequestParam(value = "projectId", required = false) Long projectFilterId,
            RedirectAttributes redirectAttributes, Model model) {

        Credentials credentials = getCurrentCredentials();
        if (credentials == null) {
            return "redirect:/login";
        }

        validatePasswords(credentialsDto, bindingResult);

        if (!bindingResult.hasErrors()) {
            String candidatePersistedUsername = buildPersistedUsername(credentials, credentialsDto.getVisibleUsername());
            credentialsService.findByUsernameAndIdNot(candidatePersistedUsername, credentials.getId())
                    .ifPresent(existing -> bindingResult.rejectValue("visibleUsername",
                            "credentials.visibleUsername.duplicate"));
        }

        if (bindingResult.hasErrors()) {
            ensureUserForm(model, credentials);
            populateCommon(model, credentials, projectFilterId);
            return PERSONAL_AREA_VIEW;
        }

        credentialsService.updateCredentials(credentials, credentialsDto.getVisibleUsername(),
                credentialsDto.getNewPassword());

        String successMessage = resolveMessage("profile.credentials.success");
        redirectAttributes.addFlashAttribute("credentialsSuccessMessage", successMessage);
        if (projectFilterId != null) {
            redirectAttributes.addAttribute("projectId", projectFilterId);
        }
        return "redirect:/personal-area";
    }

    private void populateCommon(Model model, Credentials credentials, Long projectFilterId) {
        model.addAttribute("user", credentials);
        boolean isSuperadmin = Credentials.SUPERADMIN_ROLE.equals(credentials.getRole());
        Long currentProjectId = isSuperadmin ? null : credentials.getProjectId();
        model.addAttribute("currentProjectId", currentProjectId);
        model.addAttribute("isSuperadmin", isSuperadmin);
        model.addAttribute("selectedProjectId", projectFilterId);

        if (isSuperadmin) {
            List<Project> projects = new ArrayList<>();
            projectService.getAllProjects().forEach(projects::add);
            model.addAttribute("projects", projects);
            if (projectFilterId != null) {
                model.addAttribute("projectUsers", credentialsService.findByProjectIdAndUserIsNotNull(projectFilterId));
            } else {
                model.addAttribute("projectUsers", new ArrayList<Credentials>());
            }
        }
    }

    private void ensureUserForm(Model model, Credentials credentials) {
        if (!model.containsAttribute("userForm")) {
            User user = credentials.getUser();
            if (user == null) {
                user = new User();
            }
            model.addAttribute("userForm", user);
        }
    }

    private void ensureCredentialsDto(Model model, Credentials credentials) {
        if (!model.containsAttribute("credentialsDto")) {
            CredentialsUpdateDTO dto = new CredentialsUpdateDTO();
            String visibleUsername = credentials.getVisibleUsername();
            if (!StringUtils.hasText(visibleUsername)) {
                visibleUsername = extractVisibleUsername(credentials.getUsername());
            }
            dto.setVisibleUsername(visibleUsername);
            model.addAttribute("credentialsDto", dto);
        }
    }

    private void validatePasswords(CredentialsUpdateDTO credentialsDto, BindingResult bindingResult) {
        String newPassword = credentialsDto.getNewPassword();
        String confirmPassword = credentialsDto.getConfirmPassword();

        boolean hasNewPassword = StringUtils.hasText(newPassword);
        boolean hasConfirmPassword = StringUtils.hasText(confirmPassword);

        if (hasNewPassword || hasConfirmPassword) {
            if (!hasNewPassword || !hasConfirmPassword || !newPassword.equals(confirmPassword)) {
                bindingResult.rejectValue("confirmPassword", "credentials.password.mismatch");
            }
        }
    }

    private Credentials getCurrentCredentials() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            return null;
        }
        return credentialsService.getCredentials(userDetails.getUsername());
    }

    private String buildPersistedUsername(Credentials credentials, String visibleUsername) {
        if (!StringUtils.hasText(visibleUsername)) {
            return visibleUsername;
        }

        String suffix = Credentials.SUPERADMIN_ROLE;
        if (!Credentials.SUPERADMIN_ROLE.equals(credentials.getRole())) {
            Long projectId = credentials.getProjectId();
            if (projectId != null) {
                Project project = projectService.getProjectById(projectId);
                if (project != null && StringUtils.hasText(project.getName())) {
                    suffix = project.getName();
                } else {
                    suffix = "";
                }
            } else {
                suffix = "";
            }
        }

        return StringUtils.hasText(suffix) ? visibleUsername + "|" + suffix : visibleUsername;
    }

    private String extractVisibleUsername(String persistedUsername) {
        if (!StringUtils.hasText(persistedUsername)) {
            return persistedUsername;
        }
        int separatorIndex = persistedUsername.indexOf('|');
        if (separatorIndex >= 0) {
            return persistedUsername.substring(0, separatorIndex);
        }
        return persistedUsername;
    }

    private String resolveMessage(String code) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, null, locale);
    }
}
