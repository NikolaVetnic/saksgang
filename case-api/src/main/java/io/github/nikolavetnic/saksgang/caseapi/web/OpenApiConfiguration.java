package io.github.nikolavetnic.saksgang.caseapi.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

	@Bean
	OpenAPI caseApiOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Saksgang case API")
				.description("Grant applications from organizations, and case processing by case officers.")
				.version("v1"));
	}

}
