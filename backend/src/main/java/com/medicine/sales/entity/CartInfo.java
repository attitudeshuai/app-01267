package com.medicine.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cart_info")
public class CartInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long purchaserId;
    private Long medicineId;
    private Long merchantId;
    private Integer quantity;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
