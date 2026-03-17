package it.sensorplatform.controller.rest;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.service.PacketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

    @Value("${app.api-key}") 
    private String expectedApiKey;

    public PacketControllerRest(PacketService packetService) {
        this.packetService = packetService;
    }
    

    

    @PostMapping
    public ResponseEntity<PacketService.Result> handle(@RequestBody PacketDTO packet, @RequestHeader (value = "X-API-Key", required = false) String apiKey) {
        if (apiKey == null || !apiKey.equals(expectedApiKey)) {
            System.out.println("PacketControllerRest.handle - invalid or missing API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        /*if(Duration.between(istant.now(), PacketDTO.payloa.timestamp) >= 5){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            
        }*/ //placeholder per un futuro timer (questo è sbagliato per ora)
        System.out.println("PacketControllerRest.handle - received packet: " + packet);
        PacketService.Result result = packetService.handlePacket(packet);
        System.out.println("PacketControllerRest.handle - processing result: " + result);
        return ResponseEntity.ok(result);
    }

}

