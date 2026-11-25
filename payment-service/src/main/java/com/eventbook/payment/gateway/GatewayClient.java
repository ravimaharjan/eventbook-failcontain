package com.eventbook.payment.gateway;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class GatewayClient {

    private final RestClient restClient;

    public GatewayClient(
            RestClient.Builder builder,
            @Value("${app.gateway.base-url}") String baseUrl,
            @Value("${app.gateway.connect-timeout:1s}") Duration connectTimeout,
            @Value("${app.gateway.read-timeout:2s}") Duration readTimeout) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Bulkhead(name = "gatewayCharge", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "chargeFallback")
    @CircuitBreaker(name = "gatewayCharge", fallbackMethod = "chargeFallback")
    public GatewayResponse charge(ChargeRequest request) {
        return restClient.post()
                .uri("/charge")
                .header("Idempotency-Key", request.bookingId().toString())
                .body(request)
                .retrieve()
                .body(GatewayResponse.class);
    }

    private GatewayResponse chargeFallback(ChargeRequest request, Throwable failure) {
        if (failure instanceof HttpClientErrorException clientError) {
            throw clientError;
        }
        if (failure instanceof CallNotPermittedException
                || failure instanceof io.github.resilience4j.bulkhead.BulkheadFullException) {
            throw new RetryableGatewayException("Gateway call deferred by resilience protection", failure);
        }
        throw new RetryableGatewayException("Gateway call failed and will be retried", failure);
    }
}
