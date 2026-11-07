package org.vidat.orderservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vidat.orderservice.clients.StockClient;
import org.vidat.orderservice.dtos.OrderLineRequest;
import org.vidat.orderservice.dtos.OrderRequest;
import org.vidat.orderservice.dtos.OrderResponse;
import org.vidat.orderservice.exceptions.InsufficientStockException;
import org.vidat.orderservice.exceptions.OrderNotFoundException;
import org.vidat.orderservice.models.Order;
import org.vidat.orderservice.models.OrderLine;
import org.vidat.orderservice.repositories.OrderRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final StockClient stockClient;

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {

        // 6.3: check every line first, change nothing yet
        for (OrderLineRequest line : orderRequest.getLines()) {
            if (!stockClient.isAvailable(line.getSku(), line.getQuantity())) {
                throw new InsufficientStockException(line.getSku(), line.getQuantity());
            }
        }

        // build and save the order
        Order order = new Order();
        for (OrderLineRequest lineRequest : orderRequest.getLines()) {
            OrderLine orderLine = new OrderLine();
            orderLine.setSku(lineRequest.getSku());
            orderLine.setQuantity(lineRequest.getQuantity());
            orderLine.setUnitPrice(BigDecimal.ZERO);   // TODO: get the real price from catalog-service
            order.addLine(orderLine);
        }
        Order saved = orderRepository.save(order);

        // 6.4: reduce stock after saving
        for (OrderLineRequest line : orderRequest.getLines()) {
            stockClient.reduce(line.getSku(), line.getQuantity());
        }

        return OrderResponse.from(saved);
    }

    public List<OrderResponse> getOrder() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return OrderResponse.from(order);
    }
}