package it.sensorplatform.controller.rest;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.service.PacketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint that receives generic JSON packets and delegates the
 * handling to {@link PacketService} which implements the filtering logic.
 */
@RestController
@RequestMapping("/api/packets")
public class PacketControllerRest {

    private final PacketService packetService;

    public PacketControllerRest(PacketService packetService) {
        this.packetService = packetService;
    }

    @PostMapping
    public ResponseEntity<PacketService.Result> handle(@RequestBody PacketDTO packet) {
        PacketService.Result result = packetService.handlePacket(packet);
        return ResponseEntity.ok(result);
    }
}

