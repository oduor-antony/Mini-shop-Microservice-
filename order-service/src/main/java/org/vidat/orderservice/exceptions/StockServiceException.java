package org.vidat.orderservice.exceptions;

public class StockServiceException extends RuntimeException {
    public StockServiceException(String message) {
        super(message);
    }
}