package org.mifos.processor.bulk.camel.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_AMOUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_COUNT;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mifos.processor.bulk.schema.BatchAccountLookupResponseDTO;
import org.mifos.processor.bulk.schema.BeneficiaryDTO;
import org.mifos.processor.bulk.schema.Transaction;

/**
 * AccountLookupCallbackRoute is a singleton. Before PHEE-458 it kept the approved count and amount in instance fields
 * that every callback reset, filled and read back, so overlapping callbacks for different batches could publish each
 * other's totals.
 */
class AccountLookupCallbackRouteTotalsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CamelContext camelContext;

    @BeforeEach
    void setUp() throws Exception {
        AccountLookupCallbackRoute route = new AccountLookupCallbackRoute();
        route.objectMapper = MAPPER;
        camelContext = new DefaultCamelContext();
        camelContext.addRoutes(route);
        // The file update and upload steps aren't under test.
        AdviceWith.adviceWith(camelContext, "direct:batch-account-lookup-callback", advice -> advice.weaveByToUri("direct:*").remove());
        camelContext.start();
    }

    @AfterEach
    void tearDown() {
        camelContext.stop();
    }

    @Test
    void countsApprovedAndSkipsUnknownPayees() throws Exception {
        Exchange exchange = callback(3, 2, 7);
        assertThat(exchange.getProperty(PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_COUNT)).isEqualTo(3);
        assertThat(exchange.getProperty(PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_AMOUNT)).isEqualTo(21);
    }

    @Test
    void concurrentCallbacksKeepTheirOwnTotals() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 2000; i++) {
                int approved = 1 + i % 50;
                Callable<String> task = () -> {
                    Exchange exchange = callback(approved, 1, 1);
                    Object count = exchange.getProperty(PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_COUNT);
                    Object amount = exchange.getProperty(PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_AMOUNT);
                    return count.equals(approved) && amount.equals(approved) ? null
                            : "expected " + approved + ", got count " + count + " amount " + amount;
                };
                results.add(pool.submit(task));
            }
            List<String> wrong = new ArrayList<>();
            for (Future<String> result : results) {
                if (result.get() != null) {
                    wrong.add(result.get());
                }
            }
            assertThat(wrong).as("callbacks that published another batch's totals").isEmpty();
        } finally {
            pool.shutdownNow();
        }
    }

    /** One callback for a batch of {@code approved + unknown} transactions, each of {@code amount}. */
    private Exchange callback(int approved, int unknown, int amount) throws Exception {
        List<Transaction> transactions = new ArrayList<>();
        List<BeneficiaryDTO> beneficiaries = new ArrayList<>();
        for (int i = 0; i < approved + unknown; i++) {
            Transaction transaction = new Transaction();
            transaction.setPayeeIdentifier("payee-" + i);
            transaction.setAmount(String.valueOf(amount));
            transactions.add(transaction);
            if (i < approved) {
                beneficiaries.add(new BeneficiaryDTO("payee-" + i, "00", "account-" + i, "dfsp"));
            }
        }
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(SERVER_FILE_NAME, "batch.csv");
        exchange.setProperty(TRANSACTION_LIST, transactions);
        exchange.setProperty("workflowInstanceKey", 1L);
        exchange.setProperty("batchAccountLookupCallback",
                MAPPER.writeValueAsString(new BatchAccountLookupResponseDTO("request", "institution", beneficiaries)));
        camelContext.createProducerTemplate().send("direct:batch-account-lookup-callback", exchange);
        assertThat(exchange.getException()).isNull();
        return exchange;
    }
}
