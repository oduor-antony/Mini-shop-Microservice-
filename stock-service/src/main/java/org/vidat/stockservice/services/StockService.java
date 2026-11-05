package org.vidat.stockservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.vidat.stockservice.dto.StockRequest;
import org.vidat.stockservice.dto.StockResponse;
import org.vidat.stockservice.exceptions.InsufficientStockException;
import org.vidat.stockservice.exceptions.StockNotFoundException;
import org.vidat.stockservice.models.StockItem;
import org.vidat.stockservice.repositories.StockRepository;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;

    public StockResponse setQuantity(StockRequest request){
        StockItem item=stockRepository.findBySku(request.getSku()).orElseGet(()->new StockItem(null, request.getSku(),0)
        );
        item.setQuantity(request.getQuantity());
        StockItem saved=stockRepository.save(item);
        return new StockResponse(saved.getSku(), saved.getQuantity());
    }

    public StockResponse getStock(String sku){
        StockItem item2=stockRepository.findBySku(sku).orElseThrow(()->new StockNotFoundException(sku));
        return new StockResponse(item2.getSku(),item2.getQuantity());
    }

    public boolean isAvailable(String sku,Integer qty){
        return stockRepository.findBySku(sku).map(s->s.getQuantity()>=qty)
                .orElse(false);
    }
    public StockItem reduce(String sku, Integer qty){
        StockItem stock=stockRepository.findBySku(sku).orElseThrow();
        if(stock.getQuantity()>qty){
            throw new InsufficientStockException();
        }
        stock.setQuantity(stock.getQuantity()-qty);
        return stockRepository.save(stock);
    }
}
