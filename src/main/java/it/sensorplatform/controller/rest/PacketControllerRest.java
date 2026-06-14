package it.sensorplatform.controller.rest;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Project;
import it.sensorplatform.service.PacketService;
import it.sensorplatform.service.ProjectService;
import it.sensorplatform.util.ApiKeyUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

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

        // Verifica API key SHA-256
        String incomingHash = ApiKeyUtil.hashKey(apiKey);
        if (!incomingHash.equals(project.getApiKeyHash())) {
            System.out.println("PacketControllerRest.handle - invalid API key for project " + project.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Verifica HMAC se presente
        if (hmacSignature != null && !hmacSignature.isBlank()) {
            try {
                String rawBody = httpRequest.getAttribute("rawBody") != null
                        ? (String) httpRequest.getAttribute("rawBody")
                        : readBody(httpRequest);

                if (!ApiKeyUtil.verifyHmac(rawBody, apiKey, hmacSignature)) {
                    System.out.println("PacketControllerRest.handle - invalid HMAC for project " + project.getName());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
            } catch (IOException e) {
                System.out.println("PacketControllerRest.handle - error reading body for HMAC");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }

        System.out.println("PacketControllerRest.handle - received packet: " + packet);
        PacketService.Result result = packetService.handlePacket(packet);
        System.out.println("PacketControllerRest.handle - processing result: " + result);
        return ResponseEntity.ok(result);
    }

    private String readBody(HttpServletRequest request) throws IOException {
        try (BufferedReader reader = request.getReader()) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}