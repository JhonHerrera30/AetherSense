package it.sensorplatform.controller;

import it.sensorplatform.dto.AlertConfigDTO;
import it.sensorplatform.model.AlertConfigGlobal;
import it.sensorplatform.model.AlertConfigSignal;
import it.sensorplatform.model.Project;
import it.sensorplatform.repository.AlertConfigGlobalRepository;
import it.sensorplatform.repository.AlertConfigSignalRepository;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.util.AlertDefaults;
import it.sensorplatform.service.CredentialsService;
import it.sensorplatform.model.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;

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
        AlertConfigGlobal global = globalRepo.findByProjectId(projectId).orElse(null);
        dto.setGlobalIntervalMin(global != null ? global.getIntervalMin() : 30);
        dto.setTelegramChatId(global != null ? global.getTelegramChatId() : null);
        dto.setTelegramInviteLink(global != null ? global.getTelegramInviteLink() : null);

        List<AlertConfigDTO.SignalConfig> signalDtos = new ArrayList<>();

        // segnali numerici con default
        // segnali numerici con default
        List<String> numericSignals = List.of(
                "temperature_celsius", "humidity_percent", "co2concentration_ppm",
                "pressure_hpa", "gasresistance_ohm", "voc_index", "nox_index",
                "pm1_0_ugm3", "pm2_5_ugm3", "pm4_0_ugm3", "pm10_0_ugm3",
                "si_m_s", "pga_m_s2");

        // recupera il projectKey dal progetto
        String projectKey = "default";
        try {
            Project proj = projectService.getProjectById(projectId);
            if (proj != null && proj.getName() != null) {
                projectKey = proj.getName().toLowerCase();
            }
        } catch (Exception ignored) {
        }

        final String finalProjectKey = projectKey;

        for (String signalKey : numericSignals) {
            AlertConfigSignal saved = signalRepo
                    .findByProjectIdAndSignalKey(projectId, signalKey).orElse(null);
            AlertConfigDTO.SignalConfig sc = new AlertConfigDTO.SignalConfig();
            sc.setSignalKey(signalKey);
            if (saved != null) {
                sc.setThresholdWarning(saved.getThresholdWarning());
                sc.setThresholdCritical(saved.getThresholdCritical());
                sc.setThresholdWarningLow(saved.getThresholdWarningLow());
                sc.setThresholdCriticalLow(saved.getThresholdCriticalLow());
                sc.setTriggerValue(saved.getTriggerValue());
                sc.setIntervalMin(saved.getIntervalMin());
            } else {
                AlertDefaults.Thresholds def = AlertDefaults.get(finalProjectKey, signalKey);
                sc.setThresholdWarning(def != null ? def.warningHigh() : null);
                sc.setThresholdCritical(def != null ? def.criticalHigh() : null);
                sc.setThresholdWarningLow(def != null ? def.warningLow() : null);
                sc.setThresholdCriticalLow(def != null ? def.criticalLow() : null);
                sc.setTriggerValue(null);
                sc.setIntervalMin(null);
            }
            signalDtos.add(sc);
        }
        ;

        // booleani — default triggerValue=1 (alert attivo su fault)
        for (String key : List.of("earthquake_flag", "shutoff", "collapse")) {
            AlertConfigSignal saved = signalRepo
                    .findByProjectIdAndSignalKey(projectId, key).orElse(null);
            AlertConfigDTO.SignalConfig sc = new AlertConfigDTO.SignalConfig();
            sc.setSignalKey(key);
            sc.setTriggerValue(saved != null && saved.getTriggerValue() != null ? saved.getTriggerValue() : 1);
            sc.setIntervalMin(saved != null ? saved.getIntervalMin() : null);
            signalDtos.add(sc);
        }

        // stati discreti — nessun default, l'admin sceglie
        for (String key : List.of("state", "axis_state")) {
            AlertConfigSignal saved = signalRepo
                    .findByProjectIdAndSignalKey(projectId, key).orElse(null);
            AlertConfigDTO.SignalConfig sc = new AlertConfigDTO.SignalConfig();
            sc.setSignalKey(key);
            sc.setTriggerValue(saved != null ? saved.getTriggerValue() : null);
            sc.setIntervalMin(saved != null ? saved.getIntervalMin() : null);
            signalDtos.add(sc);
        }

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
        signal.setThresholdWarningLow(body.getThresholdWarningLow());
        signal.setThresholdCriticalLow(body.getThresholdCriticalLow());
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

    @GetMapping("/{projectId}/invite-link")
    public ResponseEntity<Map<String, String>> getInviteLink(
            @PathVariable Long projectId) {
        // solo verifica che l'utente appartenga al progetto, non richiede ruolo admin
        Credentials c = getCurrentCredentials();
        if (!Objects.equals(c.getProjectId(), projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        AlertConfigGlobal global = globalRepo.findByProjectId(projectId).orElse(null);
        String link = global != null ? global.getTelegramInviteLink() : null;
        if (link == null || link.isBlank()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("inviteLink", link));
    }
}