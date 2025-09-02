package it.sensorplatform.controller.rest;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.sensorplatform.model.Device;
import it.sensorplatform.model.MeasurementRecord;
import it.sensorplatform.service.DeviceService;

@RestController
@RequestMapping("/api/device")
public class DeviceControllerRest {
	
	@Autowired
	private DeviceService deviceService;


	@PostMapping("/data")
	public ResponseEntity<String> receivePayload(@RequestBody Map<String, Object> data) {
	    String macAddress = (String) data.get("macAddress"); // Assicurati che sia presente nel pacchetto

		Device device = deviceService.findByMacAddress(macAddress);

	    try {
	        ObjectMapper mapper = new ObjectMapper();
	        String json = mapper.writeValueAsString(data);

	        MeasurementRecord measurement = new MeasurementRecord();
	        measurement.setTimeStamp(Instant.now());
	        measurement.setPayload(json);

	        device.getRecords().add(measurement);
			deviceService.save(device); // Salva anche la misurazione grazie a cascade

	        return ResponseEntity.ok("Measurement saved");
	    } catch (JsonProcessingException e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Saving failure");
	    }
	}
	
}
