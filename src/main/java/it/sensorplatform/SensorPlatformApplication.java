package it.sensorplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SensorPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(SensorPlatformApplication.class, args);
	}

}
