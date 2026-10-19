package io.github.nikolavetnic.saksgang.caseapi.organization;

import java.util.Optional;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Looks up organizations in the Central Coordinating Register for Legal Entities (Enhetsregisteret) through
 * the open API at data.brreg.no.
 */
@Component
class BrregOrganizationRegistry implements OrganizationRegistry {

	// Pin the response format so a new major version of the API cannot break us unannounced
	static final MediaType ENTITY_V2 = MediaType.parseMediaType("application/vnd.brreg.enhetsregisteret.enhet.v2+json");

	private final RestClient restClient;

	BrregOrganizationRegistry(RestClient.Builder restClientBuilder, BrregProperties properties) {
		var settings = HttpClientSettings.defaults()
				.withTimeouts(properties.connectTimeout(), properties.readTimeout());
		this.restClient = restClientBuilder
				.baseUrl(properties.baseUrl().toString())
				.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
				.build();
	}

	@Override
	public Optional<Organization> findByOrganizationNumber(String organizationNumber) {
		try {
			var entity = restClient.get()
					.uri("/enhetsregisteret/api/enheter/{organizationNumber}", organizationNumber)
					.accept(ENTITY_V2)
					.retrieve()
					.body(BrregEntity.class);
			if (entity == null) {
				throw new OrganizationRegistryUnavailableException(organizationNumber, null);
			}
			return Optional.of(entity.toOrganization());
		}
		catch (HttpClientErrorException.NotFound | HttpClientErrorException.Gone e) {
			// 410 means the entity was removed for legal reasons and should be treated as if it never existed
			return Optional.empty();
		}
		catch (RestClientException e) {
			throw new OrganizationRegistryUnavailableException(organizationNumber, e);
		}
	}

}
