package org.vidat.orderservice.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.vidat.orderservice.exceptions.InsufficientStockException;
import org.vidat.orderservice.exceptions.StockServiceException;

@Slf4j
@Component
public class StockClient {

    private static final String STOCK_SERVICE = "http://stock-service";

    private static final String UNAVAILABLE_MESSAGE =
            "We are currently unable to process your order because "
                    + "the stock service is temporarily unavailable. "
                    + "Please try again later.";

    private final RestTemplate restTemplate;

    public StockClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Read call: safe to retry. The fallback sits on @Retry so it only runs
    // after all attempts have failed.
    @Retry(name = "stockService", fallbackMethod = "isAvailableFallback")
    @CircuitBreaker(name = "stockService")
    public boolean isAvailable(String sku, int qty) {
        log.info("Calling stock-service availability for sku {}", sku);

        try {
            Boolean available = restTemplate.getForObject(
                    STOCK_SERVICE + "/stock/{sku}/available?qty={qty}",
                    Boolean.class, sku, qty
            );

            return Boolean.TRUE.equals(available);

        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new StockServiceException("Stock service is unavailable");
        }
    }

    // Fallback for stock availability: report the outage, do not pretend "out of stock"
    public boolean isAvailableFallback(String sku, int qty, Throwable throwable) {
        log.error("Stock availability check failed for SKU {}: {}", sku, throwable.toString());
        throw new StockServiceException(UNAVAILABLE_MESSAGE);
    }

    // Write call: not retried (a PUT that reduces stock is not idempotent)
    @CircuitBreaker(name = "stockService", fallbackMethod = "reduceFallback")
    public void reduce(String sku, int qty) {

        try {
            restTemplate.put(
                    STOCK_SERVICE + "/stock/{sku}/reduce?qty={qty}",
                    null, sku, qty
            );

        } catch (HttpClientErrorException.Conflict e) {
            throw new InsufficientStockException(sku, qty);

        } catch (HttpClientErrorException.NotFound e) {
            throw new StockServiceException("Unknown sku '" + sku + "' in stock service");

        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new StockServiceException("Stock service is unavailable");
        }
    }

    // Fallback for stock reduction: keep the real 409, otherwise report the outage
    public void reduceFallback(String sku, int qty, Throwable throwable) {
        if (throwable instanceof InsufficientStockException ise) {
            throw ise;
        }
        log.error("Stock reduction failed for SKU {}: {}", sku, throwable.toString());
        throw new StockServiceException(UNAVAILABLE_MESSAGE);
    }
}