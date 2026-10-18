package io.github.nikolavetnic.saksgang.sak;

import org.springframework.boot.SpringApplication;

public class TestSakApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(SakApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
