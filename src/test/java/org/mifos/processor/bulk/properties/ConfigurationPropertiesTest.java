package org.mifos.processor.bulk.properties;

import static com.google.common.truth.Truth.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * These cover what the properties records are for: the URLs come out exactly as the old {@code @PostConstruct} built
 * them, and the property names keep binding exactly as the deployment spells them.
 *
 * <p>
 * The identity_account_mapper keys use underscores in application.yaml while the prefix here is the canonical dashed
 * form. That is relaxed binding doing its job, and it is pinned below so nobody has to rename the YAML.
 */
class ConfigurationPropertiesTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ OperationsAppProperties.class, IdentityAccountMapperProperties.class })
    static class TestConfig {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    /** Exactly what application.yaml ships, underscores and all. */
    private static String[] shippedConfig() {
        return new String[] { "operations-app.contactpoint=https://ops-bk.mifos.gazelle.localhost",
                "operations-app.endpoints.batch-transaction=/api/v1/batch/transactions",
                "operations-app.endpoints.batch-summary=/api/v1/batch", "operations-app.endpoints.batch-aggregate=/api/v1/batch/",
                "operations-app.endpoints.auth=/oauth/token", "identity_account_mapper.hostname=http://ph-ee-identity-account-mapper:80",
                "identity_account_mapper.account_lookup=/beneficiary",
                "identity_account_mapper.account_lookup_callback=/accountLookupCallback",
                "identity_account_mapper.batch_account_lookup=/accountLookup",
                "identity_account_mapper.batch_account_lookup_callback=/batchAccountLookupCallback" };
    }

    @Test
    void theUnderscoredYamlKeysStillBind() {
        runner.withPropertyValues(shippedConfig()).run(context -> {
            assertThat(context.getStartupFailure()).isNull();
            IdentityAccountMapperProperties properties = context.getBean(IdentityAccountMapperProperties.class);
            assertThat(properties.hostname()).isEqualTo("http://ph-ee-identity-account-mapper:80");
            assertThat(properties.accountLookup()).isEqualTo("/beneficiary");
            assertThat(properties.accountLookupCallback()).isEqualTo("/accountLookupCallback");
            assertThat(properties.batchAccountLookup()).isEqualTo("/accountLookup");
            assertThat(properties.batchAccountLookupCallback()).isEqualTo("/batchAccountLookupCallback");
        });
    }

    @Test
    void urlsAreComposedTheWayTheOldPostConstructDidIt() {
        runner.withPropertyValues(shippedConfig()).run(context -> {
            OperationsAppProperties properties = context.getBean(OperationsAppProperties.class);
            assertThat(properties.batchTransactionUrl()).isEqualTo("https://ops-bk.mifos.gazelle.localhost/api/v1/batch/transactions");
            assertThat(properties.batchSummaryUrl()).isEqualTo("https://ops-bk.mifos.gazelle.localhost/api/v1/batch");
            assertThat(properties.batchAggregateUrl()).isEqualTo("https://ops-bk.mifos.gazelle.localhost/api/v1/batch/");
            assertThat(properties.authUrl()).isEqualTo("https://ops-bk.mifos.gazelle.localhost/oauth/token");
        });
    }

    @Test
    void theEnvironmentVariableSpellingTheDeploymentUsesStillBinds() {
        // the CR sets these two as real environment variables
        Map<String, Object> fromTheOperator = new HashMap<>();
        fromTheOperator.put("OPERATIONS_APP_CONTACTPOINT", "http://ph-ee-operations-app:80");
        fromTheOperator.put("OPERATIONS_APP_ENDPOINTS_BATCH_TRANSACTION", "/api/v1/batch/transactions");

        runner.withPropertyValues(shippedConfig())
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, fromTheOperator)))
                .run(context -> {
                    OperationsAppProperties properties = context.getBean(OperationsAppProperties.class);
                    assertThat(properties.contactpoint()).isEqualTo("http://ph-ee-operations-app:80");
                    assertThat(properties.batchTransactionUrl()).isEqualTo("http://ph-ee-operations-app:80/api/v1/batch/transactions");
                });
    }

}
