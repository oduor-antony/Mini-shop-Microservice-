package org.vidat.catalogservice.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;


@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "product")
public class Product {
    @Id
   private String id;
   private String name;
   private BigDecimal price;

   @Indexed(unique = true)
    private String sku;

}
