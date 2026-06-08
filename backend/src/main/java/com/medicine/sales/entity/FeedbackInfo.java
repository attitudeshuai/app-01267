package com.medicine.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback_info")
public class FeedbackInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long purchaserId;
    private Integer type;
    private String title;
    private String content;
    private String contact;
    private Integer status;
    private String reply;
    private LocalDateTime replyTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
