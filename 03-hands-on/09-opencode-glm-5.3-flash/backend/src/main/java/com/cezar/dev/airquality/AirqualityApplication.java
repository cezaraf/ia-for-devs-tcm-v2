package com.cezar.dev.airquality;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AirqualityApplication {

	public static void main(String[] args) {
		SpringApplication.run(AirqualityApplication.class, args);
	}

}
