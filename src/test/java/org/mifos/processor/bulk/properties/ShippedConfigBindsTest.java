package org.mifos.processor.bulk.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Every property these records ask for has to be there, or the processor must refuse to start and say which one is
 * missing. That is what the plain {@code @Value} declarations did before they were replaced, so these tests hold the
 * replacement to the same promise, and they read the real application.yaml rather than a copy of it.
 *
 * <p>
 * Every field of these records is a string, so there is no number or boolean that an empty value could break: an empty
 * value is accepted, as it was before.
 * </p>
 */
class ShippedConfigBindsTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ OperationsAppProperties.class, IdentityAccountMapperProperties.class })
    static class AllRecords {}

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OperationsAppProperties.class)
    static class OnlyOperationsApp {}

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(IdentityAccountMapperProperties.class)
    static class OnlyIdentityAccountMapper {}

    /** One configuration class per record, keyed by the prefix that record binds. */
    private static final Map<String, Class<?>> ONE_RECORD_EACH = Map.of("operations-app", OnlyOperationsApp.class,
            "identity-account-mapper", OnlyIdentityAccountMapper.class);

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(
                AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class));
    }

    private ApplicationContextRunner withShippedYaml() {
        return runner().withInitializer(new ConfigDataApplicationContextInitializer());
    }

    @Test
    void theShippedApplicationYamlFillsEveryField() {
        withShippedYaml().withUserConfiguration(AllRecords.class).run(context -> {
            assertThat(context).hasNotFailed();
            OperationsAppProperties operations = context.getBean(OperationsAppProperties.class);
            assertThat(operations.username()).isEqualTo("mifos");
            assertThat(operations.password()).isNotEmpty();
            assertThat(operations.authUrl()).isEqualTo("https://ops-bk.mifos.gazelle.localhost/oauth/token");
            assertThat(operations.batchSummaryUrl()).isEqualTo("https://ops-bk.mifos.gazelle.localhost/api/v1/batch");
            assertThat(operations.batchTransactionUrl()).isEqualTo("https://ops-bk.mifos.gazelle.localhost/api/v1/batch/transactions");
            assertThat(operations.batchAggregateUrl()).isEqualTo("https://ops-bk.mifos.gazelle.localhost/api/v1/batch/");
            // application.yaml spells these with underscores: identity_account_mapper.account_lookup and so on
            IdentityAccountMapperProperties mapper = context.getBean(IdentityAccountMapperProperties.class);
            assertThat(mapper.hostname()).isEqualTo("http://paymenthub-ee-account-mapper:80");
            assertThat(mapper.accountLookup()).isEqualTo("/beneficiary");
            assertThat(mapper.accountLookupCallback()).isEqualTo("/accountLookupCallback");
            assertThat(mapper.batchAccountLookup()).isEqualTo("/accountLookup");
            assertThat(mapper.batchAccountLookupCallback()).isEqualTo("/batchAccountLookupCallback");
        });
    }

    @Test
    void everyRecordRefusesToStartWhenItsSectionIsMissing() {
        ONE_RECORD_EACH.forEach((prefix, configuration) -> runner().withUserConfiguration(configuration).run(context -> {
            assertThat(context).as("context with nothing configured under '%s'", prefix).hasFailed();
            assertThat(context.getStartupFailure()).as("failure for '%s'", prefix).hasStackTraceContaining("BindValidationException")
                    .hasStackTraceContaining("Binding validation errors on " + prefix);
        }));
    }

    @Test
    void aMissingKeyInsideAGroupStopsStartup() {
        // only endpoints.auth is missing, so this checks that @Valid reaches the nested record
        runner().withUserConfiguration(OnlyOperationsApp.class)
                .withPropertyValues("operations-app.contactpoint=http://ops", "operations-app.username=u", "operations-app.password=p",
                        "operations-app.endpoints.batch-transaction=/t", "operations-app.endpoints.batch-summary=/s",
                        "operations-app.endpoints.batch-aggregate=/a")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("Binding validation errors on operations-app")
                            .hasStackTraceContaining("endpoints.auth");
                });
    }

    @Test
    void aValueSetToNothingOnAStringFieldIsAcceptedJustAsItWasBefore() {
        withShippedYaml().withUserConfiguration(OnlyIdentityAccountMapper.class)
                .withPropertyValues("identity_account_mapper.account_lookup_callback=").run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IdentityAccountMapperProperties.class).accountLookupCallback()).isEmpty();
                });
    }
}
