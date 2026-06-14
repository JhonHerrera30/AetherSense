package it.sensorplatform.controller.rest;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.filter.RawBodyCachingFilter;
import it.sensorplatform.model.Project;
import it.sensorplatform.service.PacketService;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.util.ApiKeyUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/packets")
public class PacketControllerRest {

    private final PacketService packetService;
    private final ProjectService projectService;

    public PacketControllerRest(PacketService packetService, ProjectService projectService) {
        this.packetService = packetService;
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<PacketService.Result> handle(
            HttpServletRequest httpRequest,
            @RequestBody PacketDTO packet,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestHeader(value = "X-HMAC-Signature", required = false) String hmacSignature) {

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("PacketControllerRest.handle - missing API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (packet.getTimestamp() == null) {
            System.out.println("PacketControllerRest.handle - missing timestamp");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (packet.getProjectId() == null) {
            System.out.println("PacketControllerRest.handle - missing projectId");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Project project = projectService.getProjectById(packet.getProjectId());
        if (project == null) {
            System.out.println("PacketControllerRest.handle - project not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (project.getApiKeyHash() == null) {
            System.out.println("PacketControllerRest.handle - project has no API key configured");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String incomingHash = ApiKeyUtil.hashKey(apiKey);
        if (!incomingHash.equals(project.getApiKeyHash())) {
            System.out.println("PacketControllerRest.handle - invalid API key for project " + project.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (hmacSignature != null && !hmacSignature.isBlank()) {
            String rawBody = null;
            if (httpRequest instanceof RawBodyCachingFilter.CachedBodyHttpServletRequest cached) {
                rawBody = cached.getRawBody();
            }
            if (rawBody == null || rawBody.isBlank()) {
                System.out.println("PacketControllerRest.handle - cannot read body for HMAC");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            if (!ApiKeyUtil.verifyHmac(rawBody, apiKey, hmacSignature)) {
                System.out.println("PacketControllerRest.handle - invalid HMAC for project " + project.getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        System.out.println("PacketControllerRest.handle - received packet: " + packet);
        PacketService.Result result = packetService.handlePacket(packet);
        System.out.println("PacketControllerRest.handle - processing result: " + result);
        return ResponseEntity.ok(result);
    }
}