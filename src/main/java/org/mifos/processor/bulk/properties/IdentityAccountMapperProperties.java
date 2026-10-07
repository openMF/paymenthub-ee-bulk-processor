package org.mifos.processor.bulk.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Everything under {@code identity_account_mapper}.
 *
 * <p>
 * The keys in application.yaml use underscores where the rest of the file uses dashes. The prefix here is the canonical
 * dashed form; Spring's relaxed binding matches it to the underscored keys, and a test pins that so the spelling in the
 * YAML can stay exactly as it is.
 *
 * <p>
 * Before this class, {@code identity_account_mapper.hostname} was read by five separate {@code @Value} fields under
 * three different names - identityURL, identityMapperURL, identityEndpoint - which is how you end up unsure whether two
 * routes are talking to the same host.
 */
@Validated
@ConfigurationProperties(prefix = "identity-account-mapper")
public record IdentityAccountMapperProperties(@NotNull String hostname, @NotNull String accountLookup,
        @NotNull String accountLookupCallback, @NotNull String batchAccountLookup, @NotNull String batchAccountLookupCallback) {
}
