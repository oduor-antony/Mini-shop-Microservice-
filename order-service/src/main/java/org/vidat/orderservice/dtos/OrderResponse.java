package org.vidat.orderservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vidat.orderservice.models.Order;
import org.vidat.orderservice.models.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.vidat.orderservice.dtos.OrderLineResponse;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private OrderStatus status;
    private Instant createdAt;
    private List<OrderLineResponse> lines;
    private BigDecimal total;

    public static OrderResponse from(Order order) {
        List<OrderLineResponse> lines = order.getLines().stream()
                .map(OrderLineResponse::from)
                .toList();

        BigDecimal total = lines.stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderResponse(order.getId(), order.getStatus(), order.getCreatedAt(), lines, total);
    }
}