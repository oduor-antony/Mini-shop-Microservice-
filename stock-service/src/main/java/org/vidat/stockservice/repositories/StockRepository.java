package org.vidat.stockservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vidat.stockservice.models.StockItem;

import java.util.Optional;

public interface StockRepository extends JpaRepository<StockItem, Long> {

    Optional<StockItem> findBySku(String sku);

    @Modifying(clearAutomatically = true)
    @Query("update StockItem s set s.quantity = s.quantity - :qty " +
            "where s.sku = :sku and s.quantity >= :qty")
    int reduceIfEnough(@Param("sku") String sku, @Param("qty") Integer qty);
}