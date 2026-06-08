package com.medicine.sales.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medicine.sales.common.Result;
import com.medicine.sales.entity.*;
import com.medicine.sales.service.AdminService;
import com.medicine.sales.service.MedicineService;
import com.medicine.sales.service.OrderService;
import com.medicine.sales.vo.DashboardVO;
import com.medicine.sales.vo.LowStockVO;
import com.medicine.sales.vo.MedicineVO;
import com.medicine.sales.vo.OrderVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;
    @Resource
    private MedicineService medicineService;
    @Resource
    private OrderService orderService;

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(adminService.getDashboard());
    }

    // ===== Merchant Management =====
    @GetMapping("/merchants")
    public Result<IPage<MerchantInfo>> merchantPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.merchantPage(current, size, status, keyword));
    }

    @PostMapping("/merchants")
    public Result<Void> createMerchant(@Valid @RequestBody MerchantInfo merchant) {
        adminService.createMerchant(merchant);
        return Result.success();
    }

    @PutMapping("/merchants")
    public Result<Void> updateMerchant(@Valid @RequestBody MerchantInfo merchant) {
        adminService.updateMerchant(merchant);
        return Result.success();
    }

    @PutMapping("/merchants/{id}/audit")
    public Result<Void> auditMerchant(@PathVariable Long id, @RequestParam Integer status) {
        adminService.auditMerchant(id, status);
        return Result.success();
    }

    @PutMapping("/merchants/{id}/status")
    public Result<Void> updateMerchantStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateMerchantStatus(id, status);
        return Result.success();
    }

    // ===== Purchaser Management =====
    @GetMapping("/purchasers")
    public Result<IPage<PurchaserInfo>> purchaserPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.purchaserPage(current, size, keyword));
    }

    @PostMapping("/purchasers")
    public Result<Void> createPurchaser(@Valid @RequestBody PurchaserInfo purchaser) {
        adminService.createPurchaser(purchaser);
        return Result.success();
    }

    @PutMapping("/purchasers")
    public Result<Void> updatePurchaser(@Valid @RequestBody PurchaserInfo purchaser) {
        adminService.updatePurchaser(purchaser);
        return Result.success();
    }

    @PutMapping("/purchasers/{id}/status")
    public Result<Void> updatePurchaserStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updatePurchaserStatus(id, status);
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
        return Result.success(medicineService.page(current, size, keyword, categoryId, null, status));
    }

    @GetMapping("/medicines/{id}")
    public Result<MedicineVO> medicineDetail(@PathVariable Long id) {
        return Result.success(medicineService.getDetail(id));
    }

    @PostMapping("/medicines")
    public Result<Void> createMedicine(@Valid @RequestBody MedicineInfo medicine) {
        medicineService.adminCreate(medicine);
        return Result.success();
    }

    @PutMapping("/medicines")
    public Result<Void> updateMedicine(@Valid @RequestBody MedicineInfo medicine) {
        medicineService.adminUpdate(medicine);
        return Result.success();
    }

    @DeleteMapping("/medicines/{id}")
    public Result<Void> deleteMedicine(@PathVariable Long id) {
        medicineService.adminDelete(id);
        return Result.success();
    }

    @PutMapping("/medicines/{id}/status")
    public Result<Void> updateMedicineStatus(@PathVariable Long id, @RequestParam Integer status) {
        medicineService.updateStatus(id, status);
        return Result.success();
    }

    // ===== Category Management =====
    @GetMapping("/categories")
    public Result<IPage<MedicineCategory>> categoryPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.categoryPage(current, size));
    }

    @GetMapping("/categories/all")
    public Result<List<MedicineCategory>> allCategories() {
        return Result.success(adminService.allCategories());
    }

    @PostMapping("/categories")
    public Result<Void> createCategory(@Valid @RequestBody MedicineCategory category) {
        adminService.createCategory(category);
        return Result.success();
    }

    @PutMapping("/categories")
    public Result<Void> updateCategory(@Valid @RequestBody MedicineCategory category) {
        adminService.updateCategory(category);
        return Result.success();
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return Result.success();
    }

    // ===== Order Management =====
    @GetMapping("/orders")
    public Result<IPage<OrderVO>> orderPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(required = false) String orderNo) {
        return Result.success(orderService.page(current, size, null, null, orderStatus, orderNo));
    }

    @GetMapping("/orders/{id}")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        return Result.success(orderService.getDetail(id));
    }

    // ===== Banner Management =====
    @GetMapping("/banners")
    public Result<List<BannerInfo>> bannerList() {
        return Result.success(adminService.bannerList());
    }

    @PostMapping("/banners")
    public Result<Void> createBanner(@Valid @RequestBody BannerInfo banner) {
        adminService.createBanner(banner);
        return Result.success();
    }

    @PutMapping("/banners")
    public Result<Void> updateBanner(@Valid @RequestBody BannerInfo banner) {
        adminService.updateBanner(banner);
        return Result.success();
    }

    @DeleteMapping("/banners/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        adminService.deleteBanner(id);
        return Result.success();
    }

    // ===== Announcement Management =====
    @GetMapping("/announcements")
    public Result<IPage<AnnouncementInfo>> announcementPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.announcementPage(current, size));
    }

    @PostMapping("/announcements")
    public Result<Void> createAnnouncement(@Valid @RequestBody AnnouncementInfo announcement) {
        adminService.createAnnouncement(announcement);
        return Result.success();
    }

    @PutMapping("/announcements")
    public Result<Void> updateAnnouncement(@Valid @RequestBody AnnouncementInfo announcement) {
        adminService.updateAnnouncement(announcement);
        return Result.success();
    }

    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        adminService.deleteAnnouncement(id);
        return Result.success();
    }

    // ===== System Config =====
    @GetMapping("/configs")
    public Result<List<SystemConfig>> configList() {
        return Result.success(adminService.configList());
    }

    @PutMapping("/configs")
    public Result<Void> updateConfig(@Valid @RequestBody SystemConfig config) {
        adminService.updateConfig(config);
        return Result.success();
    }

    // ===== Inventory & Price =====
    @GetMapping("/inventory/low-stock")
    public Result<List<LowStockVO>> lowStockList() {
        return Result.success(adminService.lowStockList());
    }

    @GetMapping("/price-history/{medicineId}")
    public Result<List<PriceRecord>> priceHistory(@PathVariable Long medicineId) {
        return Result.success(adminService.priceHistory(medicineId));
    }

    // ===== Feedback Management =====
    @GetMapping("/feedbacks")
    public Result<IPage<FeedbackInfo>> feedbackPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type) {
        return Result.success(adminService.feedbackPage(current, size, status, type));
    }

    @PutMapping("/feedbacks/{id}/reply")
    public Result<Void> replyFeedback(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        adminService.replyFeedback(id, body.getOrDefault("reply", ""));
        return Result.success();
    }

    @PutMapping("/feedbacks/{id}/close")
    public Result<Void> closeFeedback(@PathVariable Long id) {
        adminService.closeFeedback(id);
        return Result.success();
    }

    // ===== Operation Log =====
    @GetMapping("/operation-logs")
    public Result<IPage<OperationLog>> operationLogPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.operationLogPage(current, size, module, keyword));
    }
}
