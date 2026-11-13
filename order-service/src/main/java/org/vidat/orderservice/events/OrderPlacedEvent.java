package org.vidat.orderservice.events;


import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vidat.orderservice.models.Order;
// adjust to your package

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    private Long orderId;
    private Instant placedAt;
    private List<Line> lines;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private String sku;
        private Integer quantity;
    }

    public static OrderPlacedEvent from(Order order) {
        List<Line> lines = order.getLines().stream()
                .map(l -> new Line(l.getSku(), l.getQuantity()))
                .toList();
        return new OrderPlacedEvent(order.getId(), Instant.now(), lines);
    }
}
