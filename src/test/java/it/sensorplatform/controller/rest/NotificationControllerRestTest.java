package it.sensorplatform.controller.rest;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Spec;
import it.sensorplatform.model.TypeOfDevice;
import it.sensorplatform.repository.DeviceRepository;
import it.sensorplatform.repository.ProjectRepository;
import it.sensorplatform.repository.TypeOfDeviceRepository;
import it.sensorplatform.service.IndicatorService;
import it.sensorplatform.service.SpecService;
import it.sensorplatform.service.UnknownDeviceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerRestTest {

    @Test
    void ensureSpecsUsesMeasurementSegmentFromLabel() throws Exception {
        UnknownDeviceService unknownDeviceService = mock(UnknownDeviceService.class);
        DeviceRepository deviceRepository = mock(DeviceRepository.class);
        TypeOfDeviceRepository typeOfDeviceRepository = mock(TypeOfDeviceRepository.class);
        SpecService specService = mock(SpecService.class);
        IndicatorService indicatorService = mock(IndicatorService.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);

        when(specService.findByFields(any(Spec.class))).thenReturn(Optional.empty());
        when(specService.save(any(Spec.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationControllerRest controller = new NotificationControllerRest(
                unknownDeviceService,
                deviceRepository,
                typeOfDeviceRepository,
                specService,
                indicatorService,
                projectRepository
        );

        PacketDTO.SpecEntry specEntry = new PacketDTO.SpecEntry();
        specEntry.setKey("SEN55-VOC-index");
        specEntry.setLabel("SEN55-VOC-index");

        List<PacketDTO.SpecEntry> entries = List.of(specEntry);
        TypeOfDevice typeOfDevice = new TypeOfDevice();

        Method ensureSpecs = NotificationControllerRest.class
                .getDeclaredMethod("ensureSpecs", TypeOfDevice.class, List.class);
        ensureSpecs.setAccessible(true);
        ensureSpecs.invoke(controller, typeOfDevice, entries);

        ArgumentCaptor<Spec> specCaptor = ArgumentCaptor.forClass(Spec.class);
        verify(specService).save(specCaptor.capture());
        assertEquals("VOC", specCaptor.getValue().getMeasurement());
    }
}

