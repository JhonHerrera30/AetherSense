package it.sensorplatform.test;

import it.sensorplatform.controller.DeviceController;
import it.sensorplatform.dto.DeviceDTO;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.TypeOfDevice;
import it.sensorplatform.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceControllerTests {

    @Test
    void loadDeviceDTO_sortsByEmailOwnerNullsFirst() {
        TypeOfDevice tod = new TypeOfDevice();
        tod.setName("Type");

        Device deviceWithNullEmail = new Device();
        deviceWithNullEmail.setName("dev1");
        deviceWithNullEmail.setMacAddress("00:11");
        deviceWithNullEmail.setEmailOwner(null);
        deviceWithNullEmail.setDevEui("devEui1");
        deviceWithNullEmail.setTod(tod);
        deviceWithNullEmail.setId(1L);

        Device deviceB = new Device();
        deviceB.setName("dev2");
        deviceB.setMacAddress("00:22");
        deviceB.setEmailOwner("b@example.com");
        deviceB.setDevEui("devEui2");
        deviceB.setTod(tod);
        deviceB.setId(2L);

        Device deviceA = new Device();
        deviceA.setName("dev3");
        deviceA.setMacAddress("00:33");
        deviceA.setEmailOwner("a@example.com");
        deviceA.setDevEui("devEui3");
        deviceA.setTod(tod);
        deviceA.setId(3L);

        List<Device> devices = Arrays.asList(deviceB, deviceWithNullEmail, deviceA);

        DeviceController controller = new DeviceController();
        Model model = new ExtendedModelMap();
        controller.loadDeviceDTO(devices, model);

        @SuppressWarnings("unchecked")
        List<DeviceDTO> result = (List<DeviceDTO>) model.getAttribute("devices");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertNull(result.get(0).getEmailOwner());
        assertEquals("a@example.com", result.get(1).getEmailOwner());
        assertEquals("b@example.com", result.get(2).getEmailOwner());

        @SuppressWarnings("unchecked")
        List<DeviceDTO> withoutOwner = (List<DeviceDTO>) model.getAttribute("devicesWithoutOwner");
        assertNotNull(withoutOwner);
        assertEquals(1, withoutOwner.size());
        assertTrue(withoutOwner.get(0).isEmailOwnerMissing());
    }

    @Test
    void assignGsheetReturnsErrorWhenNoDevicesSelected() {
        DeviceController controller = new DeviceController();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = controller.assignGsheet(1L, null, "https://sheet", redirectAttributes);

        assertEquals("redirect:/superadmin/manageProjectDevices/1", viewName);
        assertEquals("Select at least one device to assign a Google Sheet link.",
                redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void assignGsheetReturnsErrorWhenLinkMissing() {
        DeviceController controller = new DeviceController();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = controller.assignGsheet(1L, List.of(1L), "   ", redirectAttributes);

        assertEquals("redirect:/superadmin/manageProjectDevices/1", viewName);
        assertEquals("Provide a Google Sheet link to assign to the selected devices.",
                redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void assignGsheetPersistsTrimmedLinkForSelectedDevices() {
        DeviceService deviceService = mock(DeviceService.class);
        DeviceController controller = new DeviceController();
        ReflectionTestUtils.setField(controller, "deviceService", deviceService);

        Long projectId = 7L;
        Device device1 = new Device();
        device1.setId(1L);
        Device device2 = new Device();
        device2.setId(2L);
        Device otherDevice = new Device();
        otherDevice.setId(3L);

        when(deviceService.findAllByProjectId(projectId))
                .thenReturn(new HashSet<>(Arrays.asList(device1, device2, otherDevice)));

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        List<Long> selectedDeviceIds = Arrays.asList(1L, 2L, 1L);

        String viewName = controller.assignGsheet(projectId, selectedDeviceIds, " https://sheet.example/ ", redirectAttributes);

        assertEquals("redirect:/superadmin/manageProjectDevices/" + projectId, viewName);

        verify(deviceService).findAllByProjectId(projectId);
        verify(deviceService).save(device1);
        verify(deviceService).save(device2);
        verify(deviceService, never()).save(otherDevice);

        assertEquals("https://sheet.example/", device1.getGsheet());
        assertEquals("https://sheet.example/", device2.getGsheet());
        assertNull(otherDevice.getGsheet());

        assertEquals("Google Sheet link assigned to 2 devices.",
                redirectAttributes.getFlashAttributes().get("successMessage"));
    }
}
