package de.carsten.spring_react;

import org.springframework.boot.SpringApplication;

public class TestSpringReactApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpringReactApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
