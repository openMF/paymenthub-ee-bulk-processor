package org.mifos.processor.bulk.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything under {@code operations-app}.
 *
 * <p>
 * The property names are exactly the ones that were on the {@code @Value} annotations before, because the operator sets
 * {@code OPERATIONS_APP_CONTACTPOINT} and {@code OPERATIONS_APP_ENDPOINTS_BATCH_TRANSACTION} as environment variables.
 * Renaming one would silently break a deployment.
 */
@ConfigurationProperties(prefix = "operations-app")
public record OperationsAppProperties(String contactpoint, String username, String password, Endpoints endpoints) {

    public record Endpoints(String batchTransaction, String batchSummary, String batchAggregate, String auth) {
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
