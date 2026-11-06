package org.vidat.orderservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vidat.orderservice.models.Order;

public interface OrderRepository extends JpaRepository<Order,Long> {
}
