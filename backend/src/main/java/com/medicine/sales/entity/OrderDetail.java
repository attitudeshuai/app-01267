package com.medicine.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_detail")
public class OrderDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long medicineId;
    private String medicineName;
    private String medicineImage;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private Integer rating;
    private String review;
    private LocalDateTime reviewTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
