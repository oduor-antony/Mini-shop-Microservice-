package org.vidat.orderservice.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.vidat.orderservice.dtos.OrderLineRequest;
import org.vidat.orderservice.dtos.OrderRequest;
import org.vidat.orderservice.dtos.OrderResponse;
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

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest){
        Order order=new Order();
        for(OrderLineRequest orderLineRequest:orderRequest.getLines()){
            OrderLine orderLine=new OrderLine();
            orderLine.setSku(orderLineRequest.getSku());
            orderLine.setQuantity(orderLineRequest.getQuantity());
            orderLine.setUnitPrice(BigDecimal.ZERO);   // TODO: get the real price from catalog-service
            order.addLine(orderLine);

        }
        Order saved=orderRepository.save(order);
        return OrderResponse.from(saved);
    }

    public List<OrderResponse> getOrder(){
        List<Order> order=orderRepository.findAll();
       return order.stream()
                .map(OrderResponse::from)
                .toList();
    }

    public OrderResponse getOrderById(Long id) {
      Order order=  orderRepository.findById(id).orElseThrow(()->new OrderNotFoundException(id));
      return OrderResponse.from(order);
    }
}
