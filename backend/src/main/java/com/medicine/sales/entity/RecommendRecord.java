package com.medicine.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("recommend_record")
public class RecommendRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long purchaserId;
    private Long medicineId;
    private LocalDateTime recommendTime;
    private Integer isClicked;
    private Integer isOrdered;
}
