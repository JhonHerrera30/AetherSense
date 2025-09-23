package it.sensorplatform.controller.rest;

import it.sensorplatform.dto.OperatorActivationNotification;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.service.OperatorActivationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/operators/activations")
public class OperatorActivationController {

    private final OperatorActivationService operatorActivationService;
    private final CredentialsService credentialsService;

    public OperatorActivationController(OperatorActivationService operatorActivationService,
                                        CredentialsService credentialsService) {
        this.operatorActivationService = operatorActivationService;
        this.credentialsService = credentialsService;
    }

    @GetMapping("/stream/{projectId}")
    public SseEmitter stream(@PathVariable Long projectId, Principal principal) {
        Credentials operator = requireOperator(principal);
        validateProjectAccess(operator, projectId);
        return operatorActivationService.subscribe(operator.getId(), projectId);
    }

    @GetMapping("/{projectId}")
    public List<OperatorActivationNotification> pending(@PathVariable Long projectId, Principal principal) {
        Credentials operator = requireOperator(principal);
        validateProjectAccess(operator, projectId);
        return operatorActivationService.listForOperator(projectId, operator.getId());
    }

    @PostMapping("/{projectId}/{deviceId}/consume")
    public ResponseEntity<OperatorActivationNotification> consume(@PathVariable Long projectId,
                                                                  @PathVariable Long deviceId,
                                                                  Principal principal) {
        Credentials operator = requireOperator(principal);
        validateProjectAccess(operator, projectId);
        OperatorActivationNotification notification =
                operatorActivationService.consume(projectId, deviceId, operator.getId());
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(notification);
    }

    private Credentials requireOperator(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operator not authenticated");
        }
        Credentials credentials = credentialsService.getCredentials(principal.getName());
        if (credentials == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operator not authenticated");
        }
        return credentials;
    }

    private void validateProjectAccess(Credentials operator, Long projectId) {
        if (operator.getProjectId() != null && projectId != null
                && !operator.getProjectId().equals(projectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operator cannot access this project");
        }
    }
}
