package it.sensorplatform.controller;

import it.sensorplatform.dto.AlertConfigDTO;
import it.sensorplatform.model.AlertConfigGlobal;
import it.sensorplatform.model.AlertConfigSignal;
import it.sensorplatform.model.Project;
import it.sensorplatform.repository.AlertConfigGlobalRepository;
import it.sensorplatform.repository.AlertConfigSignalRepository;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.model.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/alert-config")
public class AlertConfigController {

    @Autowired
    private AlertConfigGlobalRepository globalRepo;
    @Autowired
    private AlertConfigSignalRepository signalRepo;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private CredentialsService credentialsService;

    private Credentials getCurrentCredentials() {
        UserDetails ud = (UserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return credentialsService.getCredentials(ud.getUsername());
    }

    private void checkAccess(Long projectId) {
        Credentials c = getCurrentCredentials();
        if (!Objects.equals(c.getProjectId(), projectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Accesso negato");
        }
    }

    // GET configurazione completa per progetto
    @GetMapping("/{projectId}")
    public ResponseEntity<AlertConfigDTO> getConfig(
            @PathVariable Long projectId) {
        checkAccess(projectId);

        AlertConfigDTO dto = new AlertConfigDTO();

        // globale
        AlertConfigGlobal global = globalRepo.findByProjectId(projectId)
                .orElse(null);
        dto.setGlobalIntervalMin(global != null ? global.getIntervalMin() : 30);
        dto.setTelegramChatId(global != null ? global.getTelegramChatId() : null);
        dto.setTelegramInviteLink(global != null ? global.getTelegramInviteLink() : null);

        // segnali
        List<AlertConfigSignal> signals = signalRepo.findByProjectId(projectId);
        List<AlertConfigDTO.SignalConfig> signalDtos = signals.stream().map(s -> {
            AlertConfigDTO.SignalConfig sc = new AlertConfigDTO.SignalConfig();
            sc.setSignalKey(s.getSignalKey());
            sc.setThresholdWarning(s.getThresholdWarning());
            sc.setThresholdCritical(s.getThresholdCritical());
            sc.setTriggerValue(s.getTriggerValue());
            sc.setIntervalMin(s.getIntervalMin());
            return sc;
        }).toList();
        dto.setSignals(signalDtos);

        return ResponseEntity.ok(dto);
    }

    // PUT salva configurazione globale
    @PutMapping("/{projectId}/global")
    public ResponseEntity<Void> saveGlobal(
            @PathVariable Long projectId,
            @RequestBody AlertConfigDTO.GlobalConfig body) {
        checkAccess(projectId);

        Project project = projectService.getProjectById(projectId);
        AlertConfigGlobal global = globalRepo.findByProjectId(projectId)
                .orElse(new AlertConfigGlobal());
        global.setProject(project);
        global.setProjectId(projectId);
        global.setIntervalMin(body.getIntervalMin());
        global.setTelegramChatId(body.getTelegramChatId());
        global.setTelegramInviteLink(body.getTelegramInviteLink());
        globalRepo.save(global);
        return ResponseEntity.ok().build();
    }

    // PUT salva configurazione singolo segnale
    @PutMapping("/{projectId}/signal/{signalKey}")
    public ResponseEntity<Void> saveSignal(
            @PathVariable Long projectId,
            @PathVariable String signalKey,
            @RequestBody AlertConfigDTO.SignalConfig body) {
        checkAccess(projectId);

        AlertConfigSignal signal = signalRepo
                .findByProjectIdAndSignalKey(projectId, signalKey)
                .orElse(new AlertConfigSignal());
        signal.setProjectId(projectId);
        signal.setSignalKey(signalKey);
        signal.setThresholdWarning(body.getThresholdWarning());
        signal.setThresholdCritical(body.getThresholdCritical());
        signal.setTriggerValue(body.getTriggerValue());
        signal.setIntervalMin(body.getIntervalMin());
        signalRepo.save(signal);
        return ResponseEntity.ok().build();
    }

    // DELETE reset singolo segnale
    @DeleteMapping("/{projectId}/signal/{signalKey}")
    public ResponseEntity<Void> resetSignal(
            @PathVariable Long projectId,
            @PathVariable String signalKey) {
        checkAccess(projectId);
        signalRepo.findByProjectIdAndSignalKey(projectId, signalKey)
                .ifPresent(signalRepo::delete);
        return ResponseEntity.ok().build();
    }

    // DELETE reset timer globale al default
    @DeleteMapping("/{projectId}/global/interval")
    public ResponseEntity<Void> resetGlobalInterval(
            @PathVariable Long projectId) {
        checkAccess(projectId);
        globalRepo.findByProjectId(projectId).ifPresent(g -> {
            g.setIntervalMin(30);
            globalRepo.save(g);
        });
        return ResponseEntity.ok().build();
    }
}