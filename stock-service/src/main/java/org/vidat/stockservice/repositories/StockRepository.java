package org.vidat.stockservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vidat.stockservice.models.StockItem;

import java.util.Optional;

public interface StockRepository extends JpaRepository<StockItem, Long> {

   Optional <StockItem> findBySku(String sku);
}
