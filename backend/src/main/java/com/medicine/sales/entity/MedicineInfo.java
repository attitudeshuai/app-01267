package com.medicine.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("medicine_info")
public class MedicineInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long categoryId;
    private String origin;
    private String qualityGrade;
    private String specification;
    private String unit;
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", message = "价格不能为负")
    @DecimalMax(value = "999999.99", message = "价格不能超过999999.99")
    private BigDecimal price;
    private Long merchantId;
    private String traceCode;
    private String image;
    private String images;
    private String description;
    private String traceInfo;
    private Integer status;
    private Integer salesCount;
    /** 库存数量（仅用于管理端编辑，不持久化到 medicine_info） */
    @TableField(exist = false)
    private Integer stockQuantity;
    /** 预警阈值（仅用于管理端编辑，不持久化到 medicine_info） */
    @TableField(exist = false)
    private Integer warningThreshold;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
