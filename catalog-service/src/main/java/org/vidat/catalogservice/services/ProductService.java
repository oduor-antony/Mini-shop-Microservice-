package org.vidat.catalogservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.vidat.catalogservice.dto.ProductReponse;
import org.vidat.catalogservice.dto.ProductRequest;
import org.vidat.catalogservice.models.Product;
import org.vidat.catalogservice.repositories.ProductRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {
    private final ProductRepository productRepository;
    public String createProduct(ProductRequest productRequest){
        Product product=Product.builder()
                .name(productRequest.getName())
                .price(productRequest.getPrice())
                .sku(productRequest.getSku())
                .build();
        productRepository.save(product);
        return "product save succesfully";
    }

    public List<ProductReponse> getProducts(){
        List<Product> products=productRepository.findAll();
     return  products.stream()
                .map(u->new ProductReponse(
                        u.getId(),
                        u.getName(),
                        u.getPrice(),
                        u.getSku()
                ))
                .toList();

    }

}
