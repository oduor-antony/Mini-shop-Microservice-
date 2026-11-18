
package org.vidat.stockservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku, Integer qty) {
        super("Not enough stock for sku '" + sku + "' (requested " + qty + ")");
    }
}
