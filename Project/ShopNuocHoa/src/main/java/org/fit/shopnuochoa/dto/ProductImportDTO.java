package org.fit.shopnuochoa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fit.shopnuochoa.Enum.Gender;
import org.fit.shopnuochoa.Enum.Volume;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportDTO {
    private String name;
    private Double price;
    private String category; // Tên category (từ Excel)
    private Integer categoryId; // ID category (sau khi resolve)
    private Volume volume;
    private Gender gender;
    private Integer quantity;
    private Boolean hotTrend;
    private String imageUrl; // Sẽ được set sau khi upload ảnh
    private String imageName; // Tên ảnh được match
}

