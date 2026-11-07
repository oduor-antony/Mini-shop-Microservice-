package org.vidat.orderservice.exceptions;

public class InsufficientStockException extends RuntimeException{
    public InsufficientStockException(String sku, int requested) {
        super("Not enough stock for sku '" + sku + "' (requested " + requested + ")");
    }
}
