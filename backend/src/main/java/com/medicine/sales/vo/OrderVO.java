package com.medicine.sales.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long purchaserId;
    private String purchaserName;
    private Long merchantId;
    private String merchantName;
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private String statusText;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime receiveTime;
    private String trackingNo;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private LocalDateTime createdTime;
    private List<OrderDetailVO> details;

    @Data
    public static class OrderDetailVO {
        private Long id;
        private Long medicineId;
        private String medicineName;
        private String medicineImage;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal subtotal;
        private Integer rating;
        private String review;
        private LocalDateTime reviewTime;
    }
}
