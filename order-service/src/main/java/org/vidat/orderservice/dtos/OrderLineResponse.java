package org.vidat.orderservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vidat.orderservice.models.OrderLine;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineResponse {

    private String sku;
    private int quantity;
    private BigDecimal unitPrice;

    public static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(line.getSku(), line.getQuantity(), line.getUnitPrice());
    }
}