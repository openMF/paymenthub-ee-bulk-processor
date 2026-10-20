package org.mifos.processor.bulk.zeebe.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import jakarta.annotation.PostConstruct;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public abstract class BaseWorker {

    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    protected CsvMapper csvMapper;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${application.bucket-name}")
    private String bucketName;

    @Value("${zeebe.client.evenly-allocated-max-jobs}")
    private int workerMaxJobs;

    @Autowired
    protected CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    protected WorkerConfig workerConfig;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @PostConstruct
    public abstract void setup();

    public void newWorker(Worker worker, JobHandler handler) {
        zeebeClient.newWorker().jobType(worker.getValue()).handler(handler).name(worker.getValue()).maxJobsActive(workerMaxJobs).open();
    }

    public void sendToCamelRoute(RouteId routeId, Exchange exchange) {
        producerTemplate.send(routeId.getValue(), exchange);
    }

    /**
     * {@code producerTemplate.send} does not throw: a failing route leaves its exception on the exchange. Returns true
     * (and logs the exception) if the route failed that way, or if it set {@code failedProperty} to true.
     */
    protected boolean routeFailed(Exchange exchange, String failedProperty) {
        Exception exception = exchange.getException();
        if (exception != null) {
            logger.error("Route for exchange {} failed: {}", exchange.getExchangeId(), exception.getMessage(), exception);
            return true;
        }
        return failedProperty != null && Boolean.TRUE.equals(exchange.getProperty(failedProperty, Boolean.class));
    }

}
