package org.mifos.processor.cucumber.stepdef;

import static com.google.common.truth.Truth.assertThat;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST_ELEMENT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYMENT_MODE;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.UUID;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.connector.common.gsma.dto.GSMATransaction;
import org.mifos.processor.bulk.schema.Transaction;

public class InitRouteStepDef extends BaseStepDef {

    private static final String AMOUNT = "100";
    private static final String CURRENCY = "INR";
    private static final String PAYER = "1234567890";
    private static final String PAYEE = "0987654321";

    @Given("I can load camel context")
    public void loadCamelContext() {
        context.start();
        assertThat(template).isNotNull();
        assertThat(context).isNotNull();
    }

    @When("I call the payment-mode-validation route with {string} payment mode")
    public void callPaymentModeValidationRoute(String paymentMode) {
        exchange = template.send("direct:validate-payment-mode", exchange -> {
            exchange.setProperty(PAYMENT_MODE, paymentMode);
        });
    }

    @Then("I should get a non null exchange variable")
    public void exchangeVariableNullCheck() {
        assertThat(exchange).isNotNull();
    }

    @And("{string} exchange variable should be {string}")
    public void exchangeVariableBooleanCheck(String variableKey, String variableValue) {
        if (variableValue.equalsIgnoreCase("true")) {
            assertThat(exchange.getProperty(variableKey, Boolean.class)).isTrue();
        } else {
            assertThat(exchange.getProperty(variableKey, Boolean.class)).isFalse();
        }
    }

    @When("I call the runtime-payload route with {string} payment mode")
    public void callRuntimePayloadTestRoute(String paymentMode) {
        exchange = template.send("direct:dynamic-payload-setter", exchange -> {
            exchange.setProperty(PAYMENT_MODE, paymentMode);

            Transaction transaction = new Transaction();
            transaction.setId(0);
            transaction.setRequestId(UUID.randomUUID().toString());
            transaction.setPaymentMode(paymentMode);
            transaction.setAmount(AMOUNT);
            transaction.setPayerIdentifierType("MSISDN");
            transaction.setPayeeIdentifierType("MSISDN");
            transaction.setPayerIdentifier(PAYER);
            transaction.setPayeeIdentifier(PAYEE);
            transaction.setCurrency(CURRENCY);
            exchange.setProperty(TRANSACTION_LIST_ELEMENT, transaction);

        });

    }

    @And("The body should be of GSMA parcelable")
    public void gsmaBodyDeserializeCheck() {
        String body = exchange.getIn().getBody(String.class);
        assertThat(body).isNotNull();

        GSMATransaction gsmaTransaction;
        try {
            gsmaTransaction = objectMapper.readValue(body, GSMATransaction.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        assertThat(gsmaTransaction.getAmount()).isEqualTo(AMOUNT);
        assertThat(gsmaTransaction.getCurrency()).isEqualTo(CURRENCY);
        assertThat(gsmaTransaction.getDebitParty()[0].getValue()).isEqualTo(PAYER);
        assertThat(gsmaTransaction.getCreditParty()[0].getValue()).isEqualTo(PAYEE);
    }

    @And("The body should be of MOJALOOP parcelable")
    public void mojaloopBodyDeserializeCheck() {
        String body = exchange.getIn().getBody(String.class);
        assertThat(body).isNotNull();

        TransactionChannelRequestDTO payload;
        try {
            payload = objectMapper.readValue(body, TransactionChannelRequestDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        assertThat(payload.getAmount().getAmount()).isEqualTo(AMOUNT);
        assertThat(payload.getAmount().getCurrency()).isEqualTo(CURRENCY);
        assertThat(payload.getPayer().getPartyIdInfo().getPartyIdentifier()).isEqualTo(PAYER);
        assertThat(payload.getPayee().getPartyIdInfo().getPartyIdentifier()).isEqualTo(PAYEE);
    }
}
