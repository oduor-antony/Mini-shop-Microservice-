package org.vidat.catalogservice.controllers;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.vidat.catalogservice.dto.ProductReponse;
import org.vidat.catalogservice.dto.ProductRequest;
import org.vidat.catalogservice.models.Product;
import org.vidat.catalogservice.services.ProductService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/product")
public class ProductController {
    private final ProductService productService;
    @PostMapping("/add")
    public ResponseEntity<String> createProduct(@RequestBody ProductRequest productRequest){
        productService.createProduct(productRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("Success");
    }

    @GetMapping
    public ResponseEntity<List<ProductReponse>> getProductsList(){
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProducts());
    }
}
