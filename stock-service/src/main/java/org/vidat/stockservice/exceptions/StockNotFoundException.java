package org.vidat.stockservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class StockNotFoundException extends RuntimeException{
    public StockNotFoundException(String sku){
        super("No stock record for sku: " + sku);
    }
}
