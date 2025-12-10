package org.fit.shopnuochoa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String response;
    private List<ProductCardDTO> products;

    public ChatResponse(String response) {
        this.response = response;
        this.products = null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductCardDTO {
        private Long id;
        private String name;
        private Double price;
        private String imageUrl;
        private String brandName;
        private Double averageRating;
        private Integer ratingCount;
        private Boolean hotTrend;
    }
}

