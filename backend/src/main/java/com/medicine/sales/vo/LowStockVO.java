package com.medicine.sales.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LowStockVO {
    private Long id;
    private Long medicineId;
    private Long merchantId;
    private String name;
    private String image;
    private Integer stockQuantity;
    private Integer warningThreshold;
    private BigDecimal price;
    private String unit;
    private String categoryName;
    private String merchantName;
}
