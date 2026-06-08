package com.medicine.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("inventory_info")
public class InventoryInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long medicineId;
    private Long merchantId;
    private Integer stockQuantity;
    private Integer warningThreshold;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
