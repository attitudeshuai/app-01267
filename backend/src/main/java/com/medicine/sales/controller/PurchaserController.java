package com.medicine.sales.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medicine.sales.common.Result;
import com.medicine.sales.common.UserContext;
import com.medicine.sales.dto.OrderCreateDTO;
import com.medicine.sales.dto.ReviewDTO;
import com.medicine.sales.entity.AddressInfo;
import com.medicine.sales.entity.PurchaserInfo;
import com.medicine.sales.exception.BusinessException;
import com.medicine.sales.mapper.PurchaserMapper;
import com.medicine.sales.service.*;
import com.medicine.sales.vo.CartVO;
import com.medicine.sales.vo.MedicineVO;
import com.medicine.sales.vo.OrderVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/purchaser")
public class PurchaserController {

    @Resource
    private PurchaserMapper purchaserMapper;
    @Resource
    private MedicineService medicineService;
    @Resource
    private OrderService orderService;
    @Resource
    private CartService cartService;
    @Resource
    private AddressService addressService;
    @Resource
    private CollectionService collectionService;
    @Resource
    private com.medicine.sales.service.FeedbackService feedbackService;

    @GetMapping("/profile")
    public Result<PurchaserInfo> profile() {
        PurchaserInfo p = purchaserMapper.selectById(UserContext.getUserId());
        if (p != null) p.setPassword(null);
        return Result.success(p);
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody PurchaserInfo info) {
        PurchaserInfo p = purchaserMapper.selectById(UserContext.getUserId());
        if (p == null) throw new BusinessException("用户信息不存在");
        if (info.getNickname() != null) p.setNickname(info.getNickname());
        if (info.getPhone() != null) p.setPhone(info.getPhone());
        if (info.getEmail() != null) p.setEmail(info.getEmail());
        if (info.getAvatar() != null) p.setAvatar(info.getAvatar());
        purchaserMapper.updateById(p);
        return Result.success();
    }

    // ===== Medicine =====
    @GetMapping("/medicines")
    public Result<IPage<MedicineVO>> medicinePage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return Result.success(medicineService.page(current, size, keyword, categoryId, null, 1));
    }

    @GetMapping("/medicines/{id}")
    public Result<MedicineVO> medicineDetail(@PathVariable Long id) {
        MedicineVO vo = medicineService.getDetail(id);
        vo.setCollected(collectionService.isCollected(UserContext.getUserId(), id));
        return Result.success(vo);
    }

    @GetMapping("/medicines/hot")
    public Result<List<MedicineVO>> hotMedicines() {
        return Result.success(medicineService.getHotList());
    }

    @GetMapping("/medicines/recommend")
    public Result<List<MedicineVO>> recommendMedicines() {
        return Result.success(medicineService.getRecommendList(UserContext.getUserId()));
    }

    @PostMapping("/recommend/click")
    public Result<Void> recordRecommendClick(@RequestParam Long medicineId) {
        medicineService.recordRecommendClick(UserContext.getUserId(), medicineId);
        return Result.success();
    }

    // ===== Cart =====
    @GetMapping("/cart")
    public Result<List<CartVO>> cartList() {
        return Result.success(cartService.list(UserContext.getUserId()));
    }

    @PostMapping("/cart")
    public Result<Void> addToCart(@RequestParam Long medicineId,
                                  @RequestParam(defaultValue = "1") Integer quantity) {
        cartService.add(UserContext.getUserId(), medicineId, quantity);
        return Result.success();
    }

    @PutMapping("/cart/{id}")
    public Result<Void> updateCart(@PathVariable Long id, @RequestParam Integer quantity) {
        cartService.updateQuantity(id, quantity, UserContext.getUserId());
        return Result.success();
    }

    @DeleteMapping("/cart/{id}")
    public Result<Void> deleteCart(@PathVariable Long id) {
        cartService.delete(id, UserContext.getUserId());
        return Result.success();
    }

    @DeleteMapping("/cart")
    public Result<Void> clearCart() {
        cartService.clear(UserContext.getUserId());
        return Result.success();
    }

    // ===== Order =====
    @PostMapping("/orders")
    public Result<Long> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.createOrder(dto, UserContext.getUserId()));
    }

    @GetMapping("/orders")
    public Result<IPage<OrderVO>> orderPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer orderStatus) {
        return Result.success(orderService.page(current, size, UserContext.getUserId(), null, orderStatus, null));
    }

    @GetMapping("/orders/{id}")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        return Result.success(orderService.getDetail(id));
    }

    @PutMapping("/orders/{id}/pay")
    public Result<Void> payOrder(@PathVariable Long id) {
        orderService.payOrder(id, UserContext.getUserId());
        return Result.success();
    }

    @PutMapping("/orders/{id}/receive")
    public Result<Void> receiveOrder(@PathVariable Long id) {
        orderService.receiveOrder(id, UserContext.getUserId());
        return Result.success();
    }

    @PutMapping("/orders/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id, UserContext.getUserId());
        return Result.success();
    }

    @PostMapping("/orders/review")
    public Result<Void> reviewOrder(@Valid @RequestBody ReviewDTO dto) {
        orderService.reviewOrder(dto, UserContext.getUserId());
        return Result.success();
    }

    // ===== Address =====
    @GetMapping("/addresses")
    public Result<List<AddressInfo>> addressList() {
        return Result.success(addressService.list(UserContext.getUserId()));
    }

    @PostMapping("/addresses")
    public Result<Void> createAddress(@Valid @RequestBody AddressInfo address) {
        address.setPurchaserId(UserContext.getUserId());
        addressService.create(address);
        return Result.success();
    }

    @PutMapping("/addresses")
    public Result<Void> updateAddress(@Valid @RequestBody AddressInfo address) {
        addressService.update(address, UserContext.getUserId());
        return Result.success();
    }

    @DeleteMapping("/addresses/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        addressService.delete(id, UserContext.getUserId());
        return Result.success();
    }

    @PutMapping("/addresses/{id}/default")
    public Result<Void> setDefaultAddress(@PathVariable Long id) {
        addressService.setDefault(id, UserContext.getUserId());
        return Result.success();
    }

    // ===== Collection =====
    @GetMapping("/collections")
    public Result<IPage<MedicineVO>> collectionPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(collectionService.page(current, size, UserContext.getUserId()));
    }

    @PostMapping("/collections/{medicineId}")
    public Result<Void> toggleCollection(@PathVariable Long medicineId) {
        collectionService.toggle(UserContext.getUserId(), medicineId);
        return Result.success();
    }

    // ===== Feedback =====
    @PostMapping("/feedback")
    public Result<Void> submitFeedback(@Valid @RequestBody com.medicine.sales.entity.FeedbackInfo feedback) {
        feedbackService.submit(feedback, UserContext.getUserId());
        return Result.success();
    }
}
