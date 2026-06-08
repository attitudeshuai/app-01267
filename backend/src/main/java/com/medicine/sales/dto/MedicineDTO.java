package com.medicine.sales.dto;

import lombok.Data;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class MedicineDTO {
    private Long id;
    @NotBlank(message = "Medicine name is required")
    private String name;
    @NotNull(message = "Category is required")
    private Long categoryId;
    private String origin;
    private String qualityGrade;
    private String specification;
    private String unit;
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0", message = "价格不能为负")
    @DecimalMax(value = "999999.99", message = "价格不能超过999999.99")
    private BigDecimal price;
    private String traceCode;
    private String image;
    private String images;
    private String description;
    private String traceInfo;
    private Integer stockQuantity;
    private Integer warningThreshold;
}
