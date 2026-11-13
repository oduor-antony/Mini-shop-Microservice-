
package org.vidat.notificationservice.listeners;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vidat.notificationservice.events.OrderPlacedEvent;

@Slf4j
@Component
public class OrderEventListener {

    @KafkaListener(topics = "order-placed")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Order {} confirmed ({} line(s))", event.getOrderId(), event.getLines().size());
    }
}
