package org.mifos.processor.bulk.camel.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PAYMENT_MODE_TYPE;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST_LENGTH;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.INIT_SUB_BATCH_FAILED;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mifos.processor.bulk.config.PaymentModeType;
import org.mifos.processor.bulk.schema.Transaction;

/**
 * In PAYMENT modes InitSubBatchRoute posts each transaction of the sub-batch to channel in a loop that reuses one
 * exchange. Before PHEE-456 every response set INIT_SUB_BATCH_FAILED, so only the last transaction's HTTP status
 * counted and an earlier failure was reported as a successful sub-batch.
 */
class InitSubBatchRouteResponseTest {

    private CamelContext camelContext;
    private final Deque<Integer> responseCodes = new ArrayDeque<>();

    @BeforeEach
    void setUp() throws Exception {
        camelContext = new DefaultCamelContext();
        camelContext.addRoutes(new InitSubBatchRoute());
        // Stand in for the payload setter and the channel call: each call answers with the next queued status.
        AdviceWith.adviceWith(camelContext, "direct:start-flow-step3", route -> {
            route.weaveByToUri("direct:dynamic-payload-setter").remove();
            route.weaveByToUri("direct:external-api-call").replace()
                    .process(exchange -> exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, responseCodes.poll()));
        });
        camelContext.start();
    }

    @AfterEach
    void tearDown() {
        camelContext.stop();
    }

    @Test
    void earlierFailureIsNotClearedByALaterSuccess() {
        assertThat(initSubBatch(500, 200)).isTrue();
    }

    @Test
    void lastTransactionFailing() {
        assertThat(initSubBatch(200, 500)).isTrue();
    }

    @Test
    void allSuccessful() {
        assertThat(initSubBatch(200, 200, 200)).isFalse();
    }

    private Boolean initSubBatch(Integer... codes) {
        responseCodes.addAll(List.of(codes));
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < codes.length; i++) {
            Transaction transaction = new Transaction();
            transaction.setRequestId("request-" + i);
            transactions.add(transaction);
        }
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(PAYMENT_MODE_TYPE, PaymentModeType.PAYMENT);
        exchange.setProperty(TRANSACTION_LIST, transactions);
        exchange.setProperty(TRANSACTION_LIST_LENGTH, transactions.size());
        camelContext.createProducerTemplate().send("direct:start-workflow-step3", exchange);
        assertThat(exchange.getException()).isNull();
        assertThat(responseCodes).as("every transaction was posted").isEmpty();
        return exchange.getProperty(INIT_SUB_BATCH_FAILED, Boolean.class);
    }
}
