package it.sensorplatform.service;

import it.sensorplatform.dto.PacketDTO;
import it.sensorplatform.model.Device;
import it.sensorplatform.model.Indicator;
import it.sensorplatform.model.Spec;
import it.sensorplatform.model.TypeOfDevice;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngestServiceTests {

    @Test
    void usesSavedSpecsForMeasurementMetadata() {
        IngestService ingestService = new IngestService();

        Spec spec = new Spec();
        spec.setMeasurement("ambient_carbon");
        spec.setComponent("SEN55");
        spec.setUnitOfMeasurement("ppm");

        TypeOfDevice typeOfDevice = new TypeOfDevice();
        typeOfDevice.setName("Test");
        typeOfDevice.setSpecs(List.of(spec));

        Device device = new Device();
        device.setMacAddress("AA:BB:CC:DD:EE:FF");
        device.setTod(typeOfDevice);

        PacketDTO.SpecEntry specEntry = new PacketDTO.SpecEntry();
        specEntry.setKey("ambient_carbon");
        specEntry.setLabel("Ambient Carbon Level");
        specEntry.setMin(300.0);
        specEntry.setMax(500.0);

        ingestService.process(
                device,
                Instant.parse("2024-01-01T00:00:00Z"),
                Map.of("ambient_carbon", 420.5),
                List.of(specEntry),
                List.of()
        );

        List<IngestService.Sample> samples = ingestService.last(device.getMacAddress(), 1);
        assertEquals(1, samples.size());
        IngestService.MeasurementSample measurement = samples.get(0).measurements().get(0);

        assertEquals("ambient_carbon", measurement.key());
        assertEquals("Ambient Carbon Level", measurement.label());
        assertEquals("Ambient Carbon", measurement.displayName());
        assertEquals("ppm", measurement.unit());
        assertEquals(300.0, measurement.min());
        assertEquals(500.0, measurement.max());
        assertEquals(420.5, measurement.value());
    }

    @Test
    void fallsBackToSpecsWhenPacketDoesNotProvideLabel() {
        IngestService ingestService = new IngestService();

        Spec spec = new Spec();
        spec.setMeasurement("voc_index");
        spec.setComponent("VOC Sensor");
        spec.setUnitOfMeasurement("index");

        TypeOfDevice typeOfDevice = new TypeOfDevice();
        typeOfDevice.setName("Test 2");
        typeOfDevice.setSpecs(List.of(spec));

        Device device = new Device();
        device.setMacAddress("11:22:33:44:55:66");
        device.setTod(typeOfDevice);

        ingestService.process(
                device,
                Instant.parse("2024-02-01T00:00:00Z"),
                Map.of("voc_index", 123),
                List.of(),
                List.of()
        );

        List<IngestService.Sample> samples = ingestService.last(device.getMacAddress(), 1);
        assertEquals(1, samples.size());
        IngestService.MeasurementSample measurement = samples.get(0).measurements().get(0);

        assertEquals("voc_index", measurement.key());
        assertEquals("VOC Sensor - Voc Index", measurement.label());
        assertEquals("Voc Index", measurement.displayName());
        assertEquals("index", measurement.unit());
        assertEquals(123.0, measurement.value());
    }

    @Test
    void usesSavedIndicatorsForLabels() {
        IngestService ingestService = new IngestService();

        Indicator indicator = new Indicator();
        indicator.setKey("sen55_fan_err");
        indicator.setName("SEN55 Fan Error");

        TypeOfDevice typeOfDevice = new TypeOfDevice();
        typeOfDevice.setName("Indicator Test");
        typeOfDevice.setIndicators(List.of(indicator));

        Device device = new Device();
        device.setMacAddress("22:33:44:55:66:77");
        device.setTod(typeOfDevice);

        ingestService.process(
                device,
                Instant.parse("2024-03-01T00:00:00Z"),
                Map.of("sen55_fan_err", 1),
                List.of(),
                List.of()
        );

        List<IngestService.Sample> samples = ingestService.last(device.getMacAddress(), 1);
        assertEquals(1, samples.size());
        IngestService.IndicatorSample indicatorSample = samples.get(0).indicators().get(0);

        assertEquals("sen55_fan_err", indicatorSample.key());
        assertEquals("SEN55 Fan Error", indicatorSample.label());
        assertEquals(1, indicatorSample.value());
    }

    @Test
    void matchesMetricsIgnoringKeyCase() {
        IngestService ingestService = new IngestService();

        Spec spec = new Spec();
        spec.setMeasurement("PM10");

        TypeOfDevice typeOfDevice = new TypeOfDevice();
        typeOfDevice.setName("Case Test");
        typeOfDevice.setSpecs(List.of(spec));

        Device device = new Device();
        device.setMacAddress("33:44:55:66:77:88");
        device.setTod(typeOfDevice);

        PacketDTO.SpecEntry specEntry = new PacketDTO.SpecEntry();
        specEntry.setKey("pm10");
        specEntry.setLabel("PM10");

        ingestService.process(
                device,
                Instant.parse("2024-04-01T00:00:00Z"),
                Map.of("pM10", 12.34),
                List.of(specEntry),
                List.of()
        );

        List<IngestService.Sample> samples = ingestService.last(device.getMacAddress(), 1);
        assertEquals(1, samples.size());
        List<IngestService.MeasurementSample> measurements = samples.get(0).measurements();
        assertEquals(1, measurements.size());
        IngestService.MeasurementSample measurement = measurements.get(0);

        assertEquals("PM10", measurement.key());
        assertEquals(12.34, measurement.value());
    }
}

