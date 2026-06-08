package com.medicine.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.sales.common.Constants;
import com.medicine.sales.dto.OrderCreateDTO;
import com.medicine.sales.dto.ReviewDTO;
import com.medicine.sales.entity.*;
import com.medicine.sales.exception.BusinessException;
import com.medicine.sales.mapper.*;
import com.medicine.sales.service.MedicineService;
import com.medicine.sales.service.OrderService;
import com.medicine.sales.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderDetailMapper orderDetailMapper;
    @Resource
    private MedicineMapper medicineMapper;
    @Resource
    private InventoryMapper inventoryMapper;
    @Resource
    private AddressMapper addressMapper;
    @Resource
    private MedicineService medicineService;
    @Resource
    private MerchantNotificationMapper notificationMapper;

    @Override
    public IPage<OrderVO> page(int current, int size, Long purchaserId, Long merchantId, Integer orderStatus, String orderNo) {
        Page<OrderVO> page = new Page<>(current, size);
        IPage<OrderVO> result = orderMapper.selectOrderPage(page, purchaserId, merchantId, orderStatus, orderNo);

        List<Long> orderIds = result.getRecords().stream().map(OrderVO::getId).collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            List<OrderDetail> details = orderDetailMapper.selectList(
                    new LambdaQueryWrapper<OrderDetail>().in(OrderDetail::getOrderId, orderIds));
            Map<Long, List<OrderDetail>> detailMap = details.stream()
                    .collect(Collectors.groupingBy(OrderDetail::getOrderId));

            result.getRecords().forEach(order -> {
                List<OrderDetail> orderDetails = detailMap.getOrDefault(order.getId(), new ArrayList<>());
                List<OrderVO.OrderDetailVO> detailVOs = orderDetails.stream().map(d -> {
                    OrderVO.OrderDetailVO dvo = new OrderVO.OrderDetailVO();
                    BeanUtils.copyProperties(d, dvo);
                    return dvo;
                }).collect(Collectors.toList());
                order.setDetails(detailVOs);
                order.setStatusText(getStatusText(order.getOrderStatus()));
            });
        }
        return result;
    }

    @Override
    public OrderVO getDetail(Long id) {
        OrderInfo order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在，ID: " + id);
        }

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setStatusText(getStatusText(order.getOrderStatus()));

        List<OrderDetail> details = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id));
        List<OrderVO.OrderDetailVO> detailVOs = details.stream().map(d -> {
            OrderVO.OrderDetailVO dvo = new OrderVO.OrderDetailVO();
            BeanUtils.copyProperties(d, dvo);
            return dvo;
        }).collect(Collectors.toList());
        vo.setDetails(detailVOs);
        return vo;
    }

    @Override
    @Transactional
    public Long createOrder(OrderCreateDTO dto, Long purchaserId) {
        AddressInfo address = addressMapper.selectById(dto.getAddressId());
        if (address == null || !address.getPurchaserId().equals(purchaserId)) {
            throw new BusinessException("收货地址无效或不属于当前用户");
        }

        Map<Long, List<OrderCreateDTO.OrderItemDTO>> merchantItems = dto.getItems().stream()
                .collect(Collectors.groupingBy(item -> {
                    MedicineInfo m = medicineMapper.selectById(item.getMedicineId());
                    if (m == null || m.getStatus() != Constants.MEDICINE_STATUS_ON_SHELF) {
                        throw new BusinessException("药材不可购买，ID: " + item.getMedicineId());
                    }
                    return m.getMerchantId();
                }));

        Long firstOrderId = null;
        for (Map.Entry<Long, List<OrderCreateDTO.OrderItemDTO>> entry : merchantItems.entrySet()) {
            Long merchantId = entry.getKey();
            List<OrderCreateDTO.OrderItemDTO> items = entry.getValue();

            String orderNo = generateOrderNo();
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<OrderDetail> orderDetails = new ArrayList<>();

            for (OrderCreateDTO.OrderItemDTO item : items) {
                MedicineInfo medicine = medicineMapper.selectById(item.getMedicineId());

                InventoryInfo inv = inventoryMapper.selectOne(
                        new LambdaQueryWrapper<InventoryInfo>()
                                .eq(InventoryInfo::getMedicineId, medicine.getId())
                                .eq(InventoryInfo::getMerchantId, merchantId));
                if (inv == null || inv.getStockQuantity() < item.getQuantity()) {
                    throw new BusinessException("库存不足: " + medicine.getName() + "，当前库存: " + (inv != null ? inv.getStockQuantity() : 0));
                }

                BigDecimal subtotal = medicine.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(subtotal);

                OrderDetail detail = new OrderDetail();
                detail.setMedicineId(medicine.getId());
                detail.setMedicineName(medicine.getName());
                detail.setMedicineImage(medicine.getImage());
                detail.setQuantity(item.getQuantity());
                detail.setPrice(medicine.getPrice());
                detail.setSubtotal(subtotal);
                orderDetails.add(detail);

                inv.setStockQuantity(inv.getStockQuantity() - item.getQuantity());
                inventoryMapper.updateById(inv);

                medicine.setSalesCount(medicine.getSalesCount() + item.getQuantity());
                medicineMapper.updateById(medicine);
            }

            OrderInfo order = new OrderInfo();
            order.setOrderNo(orderNo);
            order.setPurchaserId(purchaserId);
            order.setMerchantId(merchantId);
            order.setTotalAmount(totalAmount);
            order.setOrderStatus(Constants.ORDER_STATUS_UNPAID);
            order.setReceiverName(address.getReceiverName());
            order.setReceiverPhone(address.getPhone());
            order.setReceiverAddress(address.getProvince() + address.getCity() +
                    address.getDistrict() + address.getDetailAddress());
            order.setRemark(dto.getRemark());
            orderMapper.insert(order);

            for (OrderDetail detail : orderDetails) {
                detail.setOrderId(order.getId());
                orderDetailMapper.insert(detail);
            }

            if (firstOrderId == null) {
                firstOrderId = order.getId();
            }

            MerchantNotification notif = new MerchantNotification();
            notif.setMerchantId(merchantId);
            notif.setType(3);
            notif.setTitle("新订单通知");
            notif.setContent(String.format("您有新的订单 %s，金额 ¥%.2f，请及时处理", orderNo, totalAmount));
            notif.setExtraData("{\"orderId\":" + order.getId() + ",\"orderNo\":\"" + orderNo + "\"}");
            notif.setIsRead(0);
            try { notificationMapper.insert(notif); } catch (Exception e) { log.warn("Failed to create order notification", e); }

            log.info("Order created: orderNo={}, purchaserId={}, merchantId={}, amount={}",
                    orderNo, purchaserId, merchantId, totalAmount);
        }

        return firstOrderId;
    }

    @Override
    @Transactional
    public void payOrder(Long orderId, Long purchaserId) {
        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null || !order.getPurchaserId().equals(purchaserId)) {
            throw new BusinessException("订单不存在或无权操作");
        }
        if (order.getOrderStatus() != Constants.ORDER_STATUS_UNPAID) {
            throw new BusinessException("当前订单状态不支持支付操作");
        }
        order.setOrderStatus(Constants.ORDER_STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        List<OrderDetail> details = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        List<Long> medicineIds = details.stream().map(OrderDetail::getMedicineId).distinct().collect(Collectors.toList());
        medicineService.recordRecommendOrdered(purchaserId, medicineIds);
        log.info("Order paid: orderId={}", orderId);
    }

    @Override
    public void shipOrder(Long orderId, Long merchantId, String trackingNo) {
        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException("订单不存在或无权操作");
        }
        if (order.getOrderStatus() != Constants.ORDER_STATUS_PAID) {
            throw new BusinessException("当前订单状态不支持发货操作，需要待发货状态");
        }
        order.setOrderStatus(Constants.ORDER_STATUS_SHIPPED);
        order.setShipTime(LocalDateTime.now());
        order.setTrackingNo(trackingNo);
        orderMapper.updateById(order);
        log.info("Order shipped: orderId={}, trackingNo={}", orderId, trackingNo);
    }

    @Override
    public void receiveOrder(Long orderId, Long purchaserId) {
        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null || !order.getPurchaserId().equals(purchaserId)) {
            throw new BusinessException("订单不存在或无权操作");
        }
        if (order.getOrderStatus() != Constants.ORDER_STATUS_SHIPPED) {
            throw new BusinessException("当前订单状态不支持确认收货，需要已发货状态");
        }
        order.setOrderStatus(Constants.ORDER_STATUS_RECEIVED);
        order.setReceiveTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order received: orderId={}", orderId);
    }

    @Override
    public void cancelOrder(Long orderId, Long purchaserId) {
        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null || !order.getPurchaserId().equals(purchaserId)) {
            throw new BusinessException("订单不存在或无权操作");
        }
        if (order.getOrderStatus() != Constants.ORDER_STATUS_UNPAID) {
            throw new BusinessException("只有待支付的订单才能取消");
        }

        List<OrderDetail> details = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        for (OrderDetail detail : details) {
            InventoryInfo inv = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<InventoryInfo>()
                            .eq(InventoryInfo::getMedicineId, detail.getMedicineId())
                            .eq(InventoryInfo::getMerchantId, order.getMerchantId()));
            if (inv != null) {
                inv.setStockQuantity(inv.getStockQuantity() + detail.getQuantity());
                inventoryMapper.updateById(inv);
            }
        }

        order.setOrderStatus(Constants.ORDER_STATUS_CANCELLED);
        orderMapper.updateById(order);
        log.info("Order cancelled: orderId={}", orderId);
    }

    @Override
    public void reviewOrder(ReviewDTO dto, Long purchaserId) {
        OrderDetail detail = orderDetailMapper.selectById(dto.getOrderDetailId());
        if (detail == null) {
            throw new BusinessException("订单明细不存在，ID: " + dto.getOrderDetailId());
        }
        OrderInfo order = orderMapper.selectById(detail.getOrderId());
        if (order == null || !order.getPurchaserId().equals(purchaserId)) {
            throw new BusinessException("无权评价此订单");
        }
        if (order.getOrderStatus() < Constants.ORDER_STATUS_RECEIVED) {
            throw new BusinessException("请先确认收货后再进行评价");
        }
        detail.setRating(dto.getRating());
        detail.setReview(dto.getReview());
        detail.setReviewTime(LocalDateTime.now());
        orderDetailMapper.updateById(detail);
        log.info("Order reviewed: detailId={}, rating={}", dto.getOrderDetailId(), dto.getRating());
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(9000) + 1000;
        return "ORD" + timestamp + random;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待支付";
            case 1: return "待发货";
            case 2: return "已发货";
            case 3: return "已收货";
            case 4: return "已完成";
            case 5: return "已取消";
            default: return "未知状态";
        }
    }
}
