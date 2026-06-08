package com.medicine.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("price_record")
public class PriceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long medicineId;
    private Long merchantId;
    private BigDecimal price;
    private Integer isAbnormal;
    private LocalDateTime recordTime;
}
