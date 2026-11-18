package org.vidat.orderservice.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.vidat.orderservice.exceptions.InsufficientStockException;
import org.vidat.orderservice.exceptions.StockServiceException;

@Slf4j
@Component
public class StockClient {

    private static final String UNAVAILABLE_MESSAGE =
            "We are currently unable to process your order because "
                    + "the stock service is temporarily unavailable. "
                    + "Please try again later.";

    private final RestClient restClient;

    public StockClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://stock-service").build();
    }

    // Read call: safe to retry. The fallback sits on @Retry so it only runs
    // after all attempts have failed.
    @Retry(name = "stockService", fallbackMethod = "isAvailableFallback")
    @CircuitBreaker(name = "stockService")
    public boolean isAvailable(String sku, int qty) {
        log.info("Calling stock-service availability for sku {}", sku);

        try {
            Boolean available = restClient.get()
                    .uri("/stock/{sku}/available?qty={qty}", sku, qty)
                    .retrieve()
                    .body(Boolean.class);

            return Boolean.TRUE.equals(available);

        } catch (ResourceAccessException | HttpServerErrorException
                 | IllegalStateException | IllegalArgumentException e) {
            // IllegalState/IllegalArgument: load balancer found no instance
            throw new StockServiceException("Stock service is unavailable");
        }
    }

    public boolean isAvailableFallback(String sku, int qty, Throwable throwable) {
        log.error("Stock availability check failed for SKU {}: {}", sku, throwable.toString());
        throw new StockServiceException(UNAVAILABLE_MESSAGE);
    }

    // Write call: not retried (a PUT that reduces stock is not idempotent)
    @CircuitBreaker(name = "stockService", fallbackMethod = "reduceFallback")
    public void reduce(String sku, int qty) {
        try {
            restClient.put()
                    .uri("/stock/{sku}/reduce?qty={qty}", sku, qty)
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.Conflict e) {
            throw new InsufficientStockException(sku, qty);

        } catch (HttpClientErrorException.NotFound e) {
            throw new StockServiceException("Unknown sku '" + sku + "' in stock service");

        } catch (ResourceAccessException | HttpServerErrorException
                 | IllegalStateException | IllegalArgumentException e) {
            throw new StockServiceException("Stock service is unavailable");
        }
    }

    public void reduceFallback(String sku, int qty, Throwable throwable) {
        if (throwable instanceof InsufficientStockException ise) {
            throw ise;
        }
        log.error("Stock reduction failed for SKU {}: {}", sku, throwable.toString());
        throw new StockServiceException(UNAVAILABLE_MESSAGE);
    }
}