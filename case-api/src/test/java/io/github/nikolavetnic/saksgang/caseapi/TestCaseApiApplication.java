package io.github.nikolavetnic.saksgang.caseapi;

import org.springframework.boot.SpringApplication;

public class TestCaseApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(CaseApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
