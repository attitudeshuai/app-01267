package com.medicine.sales.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchant_notification")
public class MerchantNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Integer type;
    private String title;
    private String content;
    private String extraData;
    private Integer isRead;
    private LocalDateTime createdTime;
}
