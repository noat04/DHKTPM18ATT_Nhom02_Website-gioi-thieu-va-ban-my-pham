package org.fit.shopnuochoa.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class CartItemBean implements Serializable {
    private Product product;
    private int quantity;

    public double getSubTotal() {
       return product.getPrice()*quantity;
    }
}

