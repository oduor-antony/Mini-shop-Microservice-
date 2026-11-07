package org.vidat.stockservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.vidat.stockservice.dto.StockRequest;
import org.vidat.stockservice.dto.StockResponse;
import org.vidat.stockservice.services.StockService;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockItemController {

    private final StockService stockService;

    @PostMapping
    public ResponseEntity<StockResponse> setQuantity(@RequestBody StockRequest stockRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.setQuantity(stockRequest));
    }

    @GetMapping("/{sku}")
    public ResponseEntity<StockResponse> getStock(@PathVariable String sku){
        return ResponseEntity.status(HttpStatus.OK).body(stockService.getStock(sku));
    }

    @GetMapping("/{sku}/available")
    public boolean available(@PathVariable String sku, @RequestParam Integer qty){
        return stockService.isAvailable(sku,qty);
    }
    @PutMapping("/{sku}/reduce")
    public ResponseEntity<StockResponse> reduce(@PathVariable String sku, @RequestParam Integer qty) {
        return ResponseEntity.ok(stockService.reduce(sku, qty));
    }



}
