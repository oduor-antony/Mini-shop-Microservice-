package org.vidat.stockservice.exceptions;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku, Integer qty) {
        super("Not enough stock for sku '" + sku + "' (requested " + qty + ")");
    }
}