package com.medicine.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medicine.sales.common.Constants;
import com.medicine.sales.common.Result;
import com.medicine.sales.common.UserContext;
import com.medicine.sales.dto.MedicineDTO;
import com.medicine.sales.entity.InventoryInfo;
import com.medicine.sales.entity.MedicineInfo;
import com.medicine.sales.entity.MerchantInfo;
import com.medicine.sales.entity.MerchantNotification;
import com.medicine.sales.entity.PriceRecord;
import com.medicine.sales.exception.BusinessException;
import com.medicine.sales.mapper.InventoryMapper;
import com.medicine.sales.mapper.MedicineMapper;
import com.medicine.sales.mapper.MerchantMapper;
import com.medicine.sales.mapper.MerchantNotificationMapper;
import com.medicine.sales.mapper.OrderMapper;
import com.medicine.sales.mapper.PriceRecordMapper;
import com.medicine.sales.service.MedicineService;
import com.medicine.sales.service.OrderService;
import com.medicine.sales.vo.LowStockVO;
import com.medicine.sales.vo.MedicineVO;
import com.medicine.sales.vo.OrderVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/merchant")
public class MerchantController {

    @Resource
    private MedicineService medicineService;
    @Resource
    private OrderService orderService;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private InventoryMapper inventoryMapper;
    @Resource
    private MedicineMapper medicineMapper;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private PriceRecordMapper priceRecordMapper;
    @Resource
    private MerchantNotificationMapper notificationMapper;

    @GetMapping("/profile")
    public Result<MerchantInfo> profile() {
        MerchantInfo merchant = merchantMapper.selectById(UserContext.getUserId());
        if (merchant != null) merchant.setPassword(null);
        return Result.success(merchant);
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody MerchantInfo info) {
        MerchantInfo merchant = merchantMapper.selectById(UserContext.getUserId());
        if (merchant == null) throw new BusinessException("商户信息不存在");
        if (info.getContactPerson() != null) merchant.setContactPerson(info.getContactPerson());
        if (info.getPhone() != null) merchant.setPhone(info.getPhone());
        if (info.getEmail() != null) merchant.setEmail(info.getEmail());
        if (info.getAvatar() != null) merchant.setAvatar(info.getAvatar());
        if (info.getDescription() != null) merchant.setDescription(info.getDescription());
        merchantMapper.updateById(merchant);
        return Result.success();
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Long merchantId = UserContext.getUserId();
        Map<String, Object> data = new HashMap<>();
        BigDecimal revenue = orderMapper.selectTotalRevenue(merchantId);
        data.put("totalRevenue", revenue);
        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<com.medicine.sales.entity.OrderInfo>()
                        .eq(com.medicine.sales.entity.OrderInfo::getMerchantId, merchantId));
        data.put("totalOrders", orderCount);
        Long medicineCount = medicineService.page(1, 1, null, null, merchantId, null).getTotal();
        data.put("totalMedicines", medicineCount);
        List<InventoryInfo> lowStock = inventoryMapper.selectList(
                new LambdaQueryWrapper<InventoryInfo>()
                        .eq(InventoryInfo::getMerchantId, merchantId)
                        .apply("stock_quantity <= warning_threshold"));
        data.put("lowStockCount", lowStock.size());
        long unreadCount = notificationMapper.selectCount(
                new LambdaQueryWrapper<MerchantNotification>()
                        .eq(MerchantNotification::getMerchantId, merchantId)
                        .eq(MerchantNotification::getIsRead, 0));
        data.put("unreadNotificationCount", unreadCount);
        return Result.success(data);
    }

    @GetMapping("/notifications")
    public Result<List<MerchantNotification>> notifications(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        Long merchantId = UserContext.getUserId();
        List<MerchantNotification> list = notificationMapper.selectList(
                new LambdaQueryWrapper<MerchantNotification>()
                        .eq(MerchantNotification::getMerchantId, merchantId)
                        .orderByDesc(MerchantNotification::getCreatedTime)
                        .last("LIMIT " + size));
        return Result.success(list);
    }

    @PutMapping("/notifications/{id}/read")
    public Result<Void> markNotificationRead(@PathVariable Long id) {
        MerchantNotification n = notificationMapper.selectById(id);
        if (n != null && n.getMerchantId().equals(UserContext.getUserId())) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
        return Result.success();
    }

    // ===== Medicine Management =====
    @GetMapping("/medicines")
    public Result<IPage<MedicineVO>> medicinePage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        return Result.success(medicineService.page(current, size, keyword, categoryId, UserContext.getUserId(), status));
    }

    @GetMapping("/medicines/{id}")
    public Result<MedicineVO> medicineDetail(@PathVariable Long id) {
        return Result.success(medicineService.getDetail(id));
    }

    @PostMapping("/medicines")
    public Result<Void> createMedicine(@Valid @RequestBody MedicineDTO dto) {
        medicineService.create(dto, UserContext.getUserId());
        return Result.success();
    }

    @PutMapping("/medicines")
    public Result<Void> updateMedicine(@Valid @RequestBody MedicineDTO dto) {
        medicineService.update(dto, UserContext.getUserId());
        return Result.success();
    }

    @PutMapping("/medicines/{id}/on-shelf")
    public Result<Void> onShelf(@PathVariable Long id) {
        medicineService.updateStatus(id, Constants.MEDICINE_STATUS_ON_SHELF);
        return Result.success();
    }

    @PutMapping("/medicines/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        medicineService.updateStatus(id, Constants.MEDICINE_STATUS_OFF_SHELF);
        return Result.success();
    }

    @DeleteMapping("/medicines/{id}")
    public Result<Void> deleteMedicine(@PathVariable Long id) {
        medicineService.delete(id, UserContext.getUserId());
        return Result.success();
    }

    // ===== Order Management =====
    @GetMapping("/orders")
    public Result<IPage<OrderVO>> orderPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(required = false) String orderNo) {
        return Result.success(orderService.page(current, size, null, UserContext.getUserId(), orderStatus, orderNo));
    }

    @GetMapping("/orders/{id}")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        return Result.success(orderService.getDetail(id));
    }

    @PutMapping("/orders/{id}/ship")
    public Result<Void> shipOrder(@PathVariable Long id, @RequestParam String trackingNo) {
        orderService.shipOrder(id, UserContext.getUserId(), trackingNo);
        return Result.success();
    }

    // ===== Inventory =====
    @GetMapping("/inventory/low-stock")
    public Result<List<LowStockVO>> lowStock() {
        Long merchantId = UserContext.getUserId();
        List<InventoryInfo> list = inventoryMapper.selectList(
                new LambdaQueryWrapper<InventoryInfo>()
                        .eq(InventoryInfo::getMerchantId, merchantId)
                        .apply("stock_quantity <= warning_threshold"));
        List<LowStockVO> result = new ArrayList<>();
        for (InventoryInfo inv : list) {
            MedicineInfo medicine = medicineMapper.selectById(inv.getMedicineId());
            LowStockVO vo = new LowStockVO();
            vo.setId(inv.getId());
            vo.setMedicineId(inv.getMedicineId());
            vo.setMerchantId(inv.getMerchantId());
            vo.setStockQuantity(inv.getStockQuantity());
            vo.setWarningThreshold(inv.getWarningThreshold());
            if (medicine != null) {
                vo.setName(medicine.getName());
                vo.setImage(medicine.getImage());
                vo.setPrice(medicine.getPrice());
                vo.setUnit(medicine.getUnit());
            }
            result.add(vo);
        }
        return Result.success(result);
    }

    // ===== Price History =====
    @GetMapping("/price-history/{medicineId}")
    public Result<List<PriceRecord>> priceHistory(@PathVariable Long medicineId) {
        return Result.success(priceRecordMapper.selectList(
                new LambdaQueryWrapper<PriceRecord>()
                        .eq(PriceRecord::getMedicineId, medicineId)
                        .eq(PriceRecord::getMerchantId, UserContext.getUserId())
                        .orderByAsc(PriceRecord::getRecordTime)));
    }
}
