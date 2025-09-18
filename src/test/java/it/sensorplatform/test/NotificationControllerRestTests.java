package it.sensorplatform.test;

import it.sensorplatform.controller.rest.NotificationControllerRest;
import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.dto.UnknownDeviceNotification;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Project;
import it.sensorplatform.model.TypeOfDevice;
import it.sensorplatform.repository.DeviceRepository;
import it.sensorplatform.repository.ProjectRepository;
import it.sensorplatform.repository.TypeOfDeviceRepository;
import it.sensorplatform.service.IndicatorService;
import it.sensorplatform.service.SpecService;
import it.sensorplatform.service.UnknownDeviceService;
import it.sensorplatform.util.MacAddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class NotificationControllerRestTests {

    @Test
    void addDeviceUsesDevEuiWhenMacMissing(CapturedOutput output) {
        UnknownDeviceService unknownDeviceService = mock(UnknownDeviceService.class);
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        TypeOfDeviceRepository typeOfDeviceRepository = mock(TypeOfDeviceRepository.class);
        SpecService specService = mock(SpecService.class);
        IndicatorService indicatorService = mock(IndicatorService.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);

        Long projectId = 1L;
        String key = "key";
        String normalizedKey = "KEY";
        UnknownDeviceNotification notif = new UnknownDeviceNotification(
                normalizedKey,
                null,
                "DEV123",
                projectId,
                "type",
                Map.<String, Object>of(),
                List.<PacketDTO.SpecEntry>of(),
                List.<String>of(),
                Instant.now()
        );

        when(unknownDeviceService.consume(projectId, normalizedKey)).thenReturn(notif);
        when(typeOfDeviceRepository.findByName("type")).thenReturn(Optional.of(new TypeOfDevice()));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(new Project()));
        when(deviceRepository.existsByMacAddress(MacAddressUtils.normalize("DEV123"))).thenReturn(false);
        when(deviceRepository.existsByDevEui("DEV123")).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationControllerRest controller = new NotificationControllerRest(
                unknownDeviceService,
                deviceRepository,
                typeOfDeviceRepository,
                specService,
                indicatorService,
                projectRepository
        );

        ResponseEntity<Void> response = controller.addDevice(projectId, key);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        assertNull(savedDevice.getMacAddress());
        assertEquals("DEV123", savedDevice.getDevEui());

        assertTrue(output.getOut().contains("MAC address missing; persisting device with DevEUI DEV123 only"));
    }

    @Test
    void addDeviceIgnoresNullDevEui() {
        UnknownDeviceService unknownDeviceService = mock(UnknownDeviceService.class);
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        TypeOfDeviceRepository typeOfDeviceRepository = mock(TypeOfDeviceRepository.class);
        SpecService specService = mock(SpecService.class);
        IndicatorService indicatorService = mock(IndicatorService.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);

        Long projectId = 1L;
        String key = "key";
        String normalizedKey = "KEY";
        UnknownDeviceNotification notif = new UnknownDeviceNotification(
                normalizedKey,
                "AA:BB:CC:DD:EE:FF",
                null,
                projectId,
                "type",
                Map.<String, Object>of(),
                List.<PacketDTO.SpecEntry>of(),
                List.<String>of(),
                Instant.now()
        );

        when(unknownDeviceService.consume(projectId, normalizedKey)).thenReturn(notif);
        when(typeOfDeviceRepository.findByName("type")).thenReturn(Optional.of(new TypeOfDevice()));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(new Project()));
        when(deviceRepository.existsByMacAddress(MacAddressUtils.normalize("AA:BB:CC:DD:EE:FF"))).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationControllerRest controller = new NotificationControllerRest(
                unknownDeviceService,
                deviceRepository,
                typeOfDeviceRepository,
                specService,
                indicatorService,
                projectRepository
        );

        ResponseEntity<Void> response = controller.addDevice(projectId, key);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(deviceRepository, never()).existsByDevEui(any());
    }
}

