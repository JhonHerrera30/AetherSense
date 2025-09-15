package it.sensorplatform.test;

import it.sensorplatform.controller.DeviceController;
import it.sensorplatform.dto.DeviceDTO;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.TypeOfDevice;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

        Device deviceB = new Device();
        deviceB.setName("dev2");
        deviceB.setMacAddress("00:22");
        deviceB.setEmailOwner("b@example.com");
        deviceB.setDevEui("devEui2");
        deviceB.setTod(tod);

        Device deviceA = new Device();
        deviceA.setName("dev3");
        deviceA.setMacAddress("00:33");
        deviceA.setEmailOwner("a@example.com");
        deviceA.setDevEui("devEui3");
        deviceA.setTod(tod);

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
    }
}
