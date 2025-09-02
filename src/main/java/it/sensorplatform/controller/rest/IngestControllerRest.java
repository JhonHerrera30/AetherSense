// src/main/java/it/sensorplatform/controller/rest/IngestControllerRest.java
package it.sensorplatform.controller.rest;

import it.sensorplatform.dto.TtnWebhookDTO;
import it.sensorplatform.service.IngestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
public class IngestControllerRest {

    private final IngestService ingestService;
    public IngestControllerRest(IngestService ingestService) { this.ingestService = ingestService; }

    /** Compatibile TTN/TTS webhook */
    @PostMapping("/ttn")
    public ResponseEntity<?> ingestTtn(@RequestBody TtnWebhookDTO body) {

        String deviceId = body.getEnd_device_ids() != null ? body.getEnd_device_ids().getDevice_id() : null;
        String devEui   = body.getEnd_device_ids() != null ? body.getEnd_device_ids().getDev_eui()   : null;

        Instant ts = body.getReceived_at() != null ? Instant.parse(body.getReceived_at()) : Instant.now();

        Map<String,Object> decoded = body.getUplink_message() != null
                ? body.getUplink_message().getDecoded_payload()
                : null;

        // Se decoded_payload manca, puoi opzionalmente decodificare frm_payload (base64) qui.
        // Per ora puntiamo su decoded_payload come in TTN (decoder lato TTS).
        ingestService.process(deviceId, devEui, ts, decoded);

        return ResponseEntity.accepted().build();
    }
}
