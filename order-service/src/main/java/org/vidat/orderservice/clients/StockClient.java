package org.vidat.orderservice.clients;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.vidat.orderservice.exceptions.InsufficientStockException;
import org.vidat.orderservice.exceptions.StockServiceException;

@Component
public class StockClient {

    private static final String STOCK_SERVICE = "http://stock-service";

    private final RestTemplate restTemplate;

    public StockClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isAvailable(String sku, int qty) {
        Boolean available = restTemplate.getForObject(
                STOCK_SERVICE + "/stock/{sku}/available?qty={qty}",
                Boolean.class, sku, qty);
        return Boolean.TRUE.equals(available);
    }

    public void reduce(String sku, int qty) {
        try {
            restTemplate.put(STOCK_SERVICE + "/stock/{sku}/reduce?qty={qty}", null, sku, qty);
        } catch (HttpClientErrorException.Conflict e) {
            throw new InsufficientStockException(sku, qty);          // 409
        } catch (HttpClientErrorException.NotFound e) {
            throw new StockServiceException("Unknown sku '" + sku + "' in stock service");
        } catch (ResourceAccessException e) {
            throw new StockServiceException("Stock service is unavailable");
        }
    }
}