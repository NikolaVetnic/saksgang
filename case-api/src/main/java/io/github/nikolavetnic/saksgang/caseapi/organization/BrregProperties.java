package io.github.nikolavetnic.saksgang.caseapi.organization;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("brreg")
public record BrregProperties(@NotNull URI baseUrl, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {
}
