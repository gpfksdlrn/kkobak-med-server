package com.kkobak.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KkobakMedServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(KkobakMedServerApplication.class, args);
	}

}
