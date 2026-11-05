package org.vidat.catalogservice.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.vidat.catalogservice.models.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product,String> {
}
