package org.vidat.notificationservice.events;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
