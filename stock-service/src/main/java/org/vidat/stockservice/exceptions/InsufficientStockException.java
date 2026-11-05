package org.vidat.stockservice.exceptions;

public class InsufficientStockException extends RuntimeException{
    public InsufficientStockException(){
        super("insufficient Stock");
    }
}
