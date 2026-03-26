package it.sensorplatform.controller.rest;

import it.sensorplatform.model.Admin;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Project;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class OperatorControllerRest {

    @Autowired private CredentialsService credentialsService;
    @Autowired private ProjectService projectService;

    // Semplice DTO per il browser (non è un'entità!)
    public record OperatorDTO(Long id, String visibleUsername, String email, boolean authorized) {}

    @GetMapping("/{projectId}/operators")
    public List<OperatorDTO> listOperators(@PathVariable Long projectId) {
        Project p = projectService.getProjectById(projectId);

       // Prende l'utente dalla sessione corrente
        Authentication auth = SecurityContextHolder
        .getContext().getAuthentication();
        String username = auth.getName();

        // Cerca le sue credenziali nel database
        Credentials currentUser = credentialsService
        .getCredentials(username);

        // Confronta il suo projectId con quello nell'URL
        if (!currentUser.getProjectId().equals(projectId)) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Accesso non autorizzato"
        );
        }
        String adminRole;
        if ("LTRAD".equals(p.getName()))       adminRole = Credentials.LTRAD_ADMIN_ROLE;
        else if ("FIRE".equals(p.getName()))   adminRole = Credentials.FIRE_ADMIN_ROLE;
        else if ("VOLCANO".equals(p.getName()))adminRole = Credentials.VOLCANO_ADMIN_ROLE;
        else throw new IllegalArgumentException("Unknown project: " + p.getName());

        Credentials adminCredentials = credentialsService.findByRoleAndProjectId(adminRole, projectId)
                .stream()
                .findFirst()
                .orElseThrow();
        Admin admin = adminCredentials.getAdmin();

        List<Credentials> authorized = admin.getAuthorizedOperators() != null ?
                admin.getAuthorizedOperators() : List.of();

        return (admin.getOperators() != null ? admin.getOperators() : List.<Credentials>of())
                .stream()
                .map(c -> new OperatorDTO(
                        c.getId(),
                        c.getVisibleUsername(),
                        c.getEmail(),
                        authorized.contains(c)))
                .toList();
    }
}
