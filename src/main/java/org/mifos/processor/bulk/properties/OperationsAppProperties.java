package org.mifos.processor.bulk.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Everything under {@code operations-app}.
 *
 * <p>
 * The property names are exactly the ones that were on the {@code @Value} annotations before, because the operator sets
 * {@code OPERATIONS_APP_CONTACTPOINT} and {@code OPERATIONS_APP_ENDPOINTS_BATCH_TRANSACTION} as environment variables.
 * Renaming one would silently break a deployment.
 */
@Validated
@ConfigurationProperties(prefix = "operations-app")
public record OperationsAppProperties(@NotNull String contactpoint, @NotNull String username, @NotNull String password,
        @NotNull @Valid Endpoints endpoints) {

    public record Endpoints(@NotNull String batchTransaction, @NotNull String batchSummary, @NotNull String batchAggregate,
            @NotNull String auth) {
    }

    public String batchTransactionUrl() {
        return contactpoint + endpoints.batchTransaction();
    }

    public String batchSummaryUrl() {
        return contactpoint + endpoints.batchSummary();
    }

    public String batchAggregateUrl() {
        return contactpoint + endpoints.batchAggregate();
    }

    public String authUrl() {
        return contactpoint + endpoints.auth();
    }
}
