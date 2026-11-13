package org.vidat.orderservice.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.vidat.orderservice.config.KafkaTopicConfig;

//@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @TransactionalEventListener   // default phase: AFTER_COMMIT
    public void onOrderPlaced(OrderPlacedEvent event) {
        try {
            kafkaTemplate.send(KafkaTopicConfig.ORDER_PLACED_TOPIC,
                            String.valueOf(event.getOrderId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Could not publish OrderPlacedEvent for order {}: {}",
                                    event.getOrderId(), ex.toString());
                        } else {
                            log.info("Published OrderPlacedEvent for order {} to {}-{} offset {}",
                                    event.getOrderId(),
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Could not publish OrderPlacedEvent for order {}: {}",
                    event.getOrderId(), e.toString());
        }
    }
}