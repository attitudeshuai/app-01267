package com.medicine.sales.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartVO {
    private Long id;
    private Long medicineId;
    private String medicineName;
    private String medicineImage;
    private BigDecimal price;
    private Integer quantity;
    private Long merchantId;
    private String merchantName;
    private Integer stockQuantity;
}
