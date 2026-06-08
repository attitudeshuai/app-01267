package com.medicine.sales.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MedicineVO {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String origin;
    private String qualityGrade;
    private String specification;
    private String unit;
    private BigDecimal price;
    private Long merchantId;
    private String merchantName;
    private String traceCode;
    private String image;
    private String images;
    private String description;
    private String traceInfo;
    private Integer status;
    private Integer salesCount;
    private Integer stockQuantity;
    private Integer warningThreshold;
    private Double avgRating;
    private Boolean collected;
    private LocalDateTime createdTime;
}
