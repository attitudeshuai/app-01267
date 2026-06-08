package com.medicine.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("collection_info")
public class CollectionInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long purchaserId;
    private Long medicineId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
