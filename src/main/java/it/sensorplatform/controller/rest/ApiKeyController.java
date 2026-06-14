package it.sensorplatform.controller.rest;

import it.sensorplatform.model.Project;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.util.ApiKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import it.sensorplatform.repository.ProjectRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/api-keys")
public class ApiKeyController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    // Genera o rigenera la API key per un progetto
    @PostMapping("/{projectId}/generate")
    public ResponseEntity<Map<String, String>> generateKey(@PathVariable Long projectId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperadmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SUPERADMIN"));

        if (!isSuperadmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Project project = projectService.getProjectById(projectId);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String rawKey = ApiKeyUtil.generateRawKey();
        String hash = ApiKeyUtil.hashKey(rawKey);
        project.setApiKeyHash(hash);
        projectRepository.save(project);

        // Restituisce la chiave in chiaro UNA SOLA VOLTA
        return ResponseEntity.ok(Map.of(
                "projectId", projectId.toString(),
                "projectName", project.getName(),
                "apiKey", rawKey,
                "message", "Salva questa chiave — non sarà più mostrata"));
    }

    // Verifica se un progetto ha già una chiave configurata
    @GetMapping("/{projectId}/status")
    public ResponseEntity<Map<String, Object>> keyStatus(@PathVariable Long projectId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperadmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SUPERADMIN"));

        if (!isSuperadmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Project project = projectService.getProjectById(projectId);
        boolean hasKey = project.getApiKeyHash() != null && !project.getApiKeyHash().isEmpty();

        return ResponseEntity.ok(Map.of(
                "projectId", projectId,
                "projectName", project.getName(),
                "hasApiKey", hasKey));
    }
}