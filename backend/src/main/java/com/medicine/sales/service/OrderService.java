package com.medicine.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medicine.sales.dto.OrderCreateDTO;
import com.medicine.sales.dto.ReviewDTO;
import com.medicine.sales.vo.OrderVO;

public interface OrderService {
    IPage<OrderVO> page(int current, int size, Long purchaserId, Long merchantId, Integer orderStatus, String orderNo);
    OrderVO getDetail(Long id);
    Long createOrder(OrderCreateDTO dto, Long purchaserId);
    void payOrder(Long orderId, Long purchaserId);
    void shipOrder(Long orderId, Long merchantId, String trackingNo);
    void receiveOrder(Long orderId, Long purchaserId);
    void cancelOrder(Long orderId, Long purchaserId);
    void reviewOrder(ReviewDTO dto, Long purchaserId);
}
