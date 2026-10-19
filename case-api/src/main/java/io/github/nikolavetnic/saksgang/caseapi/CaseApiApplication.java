package io.github.nikolavetnic.saksgang.caseapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CaseApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CaseApiApplication.class, args);
	}

}
