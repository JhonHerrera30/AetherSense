package it.sensorplatform.util;

import it.sensorplatform.model.AlertConfigSignal;
import it.sensorplatform.repository.AlertConfigSignalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AlertConfigInitializer implements ApplicationRunner {

    @Autowired
    private AlertConfigSignalRepository signalRepo;

    @Override
    public void run(ApplicationArguments args) {
        // VOLCANO (101)
        initNumeric(101L, "temperature_celsius", 45.0, 60.0, 0.0, -10.0);
        initNumeric(101L, "temperature", 45.0, 60.0, 0.0, -10.0);
        initNumeric(101L, "humidity_percent", 80.0, 90.0, 20.0, 10.0);
        initNumeric(101L, "co2Concentration_ppm", 1000.0, 2000.0, null, null);
        initNumeric(101L, "pressure_hpa", null, null, 950.0, 900.0);
        initNumeric(101L, "pm1_0_ugm3", 15.0, 30.0, null, null);
        initNumeric(101L, "pm2_5_ugm3", 25.0, 50.0, null, null);
        initNumeric(101L, "pm4_0_ugm3", 35.0, 70.0, null, null);
        initNumeric(101L, "pm10_0_ugm3", 50.0, 100.0, null, null);
        initNumeric(101L, "voc_index", 200.0, 350.0, null, null);
        initNumeric(101L, "nox_index", 200.0, 350.0, null, null);
        initNumeric(101L, "si_m_s", 0.5, 1.0, null, null);
        initNumeric(101L, "pga_m_s2", 0.5, 1.0, null, null);

        // LTRAD (1)
        initNumeric(1L, "temperature_celsius", 35.0, 50.0, 0.0, -5.0);
        initNumeric(1L, "temperature", 35.0, 50.0, 0.0, -5.0);
        initNumeric(1L, "humidity_percent", 80.0, 90.0, 20.0, 10.0);
        initNumeric(1L, "co2Concentration_ppm", 1000.0, 2000.0, null, null);
        initNumeric(1L, "pm2_5_ugm3", 25.0, 50.0, null, null);
        initNumeric(1L, "pm10_0_ugm3", 50.0, 100.0, null, null);

        // FIRE (51)
        initNumeric(51L, "temperature_celsius", 60.0, 80.0, null, null);
        initNumeric(51L, "temperature", 60.0, 80.0, null, null);
        initNumeric(51L, "humidity_percent", 80.0, 90.0, 20.0, 10.0);
        initNumeric(51L, "co2Concentration_ppm", 1000.0, 2000.0, null, null);
        initNumeric(51L, "pm2_5_ugm3", 25.0, 50.0, null, null);
        initNumeric(51L, "pm10_0_ugm3", 50.0, 100.0, null, null);
    }

    private void initNumeric(Long projectId, String signalKey,
            Double wHigh, Double cHigh,
            Double wLow, Double cLow) {
        if (signalRepo.findByProjectIdAndSignalKey(projectId, signalKey).isPresent())
            return;

        AlertConfigSignal s = new AlertConfigSignal();
        s.setProjectId(projectId);
        s.setSignalKey(signalKey);
        s.setThresholdWarning(wHigh);
        s.setThresholdCritical(cHigh);
        s.setThresholdWarningLow(wLow);
        s.setThresholdCriticalLow(cLow);
        s.setIntervalMin(30);
        signalRepo.save(s);
    }
}