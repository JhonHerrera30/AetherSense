package it.sensorplatform.controller.rest;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Project;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class OperatorControllerRest {

    @Autowired private CredentialsService credentialsService;
    @Autowired private ProjectService projectService;

    // Semplice DTO per il browser (non è un'entità!)
    public record OperatorDTO(Long id, String visibleUsername, String email) {}

    @GetMapping("/{projectId}/operators")
    public List<OperatorDTO> listOperators(@PathVariable Long projectId) {
        Project p = projectService.getProjectById(projectId);

        String role;
        if ("LTRAD".equals(p.getName()))       role = Credentials.LTRAD_OPERATOR_ROLE;
        else if ("FIRE".equals(p.getName()))   role = Credentials.FIRE_OPERATOR_ROLE;
        else if ("VOLCANO".equals(p.getName()))role = Credentials.VOLCANO_OPERATOR_ROLE;
        else throw new IllegalArgumentException("Unknown project: " + p.getName());

        return credentialsService.findByRoleAndProjectId(role, projectId)
                .stream()
                .map(c -> new OperatorDTO(c.getId(), c.getVisibleUsername(), c.getEmail()))
                .toList();
    }
}
