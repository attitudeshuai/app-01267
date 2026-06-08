package com.medicine.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.sales.common.Constants;
import com.medicine.sales.entity.*;
import com.medicine.sales.exception.BusinessException;
import com.medicine.sales.mapper.*;
import com.medicine.sales.service.AdminService;
import com.medicine.sales.util.PasswordUtil;
import com.medicine.sales.vo.DashboardVO;
import com.medicine.sales.vo.LowStockVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private PurchaserMapper purchaserMapper;
    @Resource
    private MedicineMapper medicineMapper;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderDetailMapper orderDetailMapper;
    @Resource
    private MedicineCategoryMapper categoryMapper;
    @Resource
    private InventoryMapper inventoryMapper;
    @Resource
    private BannerMapper bannerMapper;
    @Resource
    private AnnouncementMapper announcementMapper;
    @Resource
    private SystemConfigMapper systemConfigMapper;
    @Resource
    private PriceRecordMapper priceRecordMapper;
    @Resource
    private FeedbackMapper feedbackMapper;
    @Resource
    private OperationLogMapper operationLogMapper;
    @Resource
    private MerchantNotificationMapper merchantNotificationMapper;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setTotalOrders(orderMapper.selectCount(null));
        vo.setTotalRevenue(orderMapper.selectTotalRevenue(null));
        vo.setTotalMedicines(medicineMapper.selectCount(null));
        vo.setTotalUsers(purchaserMapper.selectCount(null));
        vo.setPendingMerchants(merchantMapper.selectCount(
                new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getStatus, Constants.MERCHANT_STATUS_PENDING)));
        vo.setPendingMedicines(medicineMapper.selectCount(
                new LambdaQueryWrapper<MedicineInfo>().eq(MedicineInfo::getStatus, Constants.MEDICINE_STATUS_PENDING)));
        vo.setLowStockCount((long) inventoryMapper.selectLowStockInventories().size());
        vo.setHotMedicines(orderDetailMapper.selectHotMedicines());
        vo.setOrderTrends(orderMapper.selectOrderTrends());
        return vo;
    }

    @Override
    public IPage<MerchantInfo> merchantPage(int current, int size, Integer status, String keyword) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(MerchantInfo::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MerchantInfo::getCompanyName, keyword)
                    .or().like(MerchantInfo::getUsername, keyword));
        }
        wrapper.orderByDesc(MerchantInfo::getCreatedTime);
        IPage<MerchantInfo> result = merchantMapper.selectPage(new Page<>(current, size), wrapper);
        result.getRecords().forEach(m -> m.setPassword(null));
        return result;
    }

    @Override
    public void auditMerchant(Long merchantId, Integer status) {
        MerchantInfo merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) throw new BusinessException("商户不存在，ID: " + merchantId);
        merchant.setStatus(status);
        merchantMapper.updateById(merchant);
        log.info("Merchant audited: id={}, status={}", merchantId, status);
    }

    private static final java.util.regex.Pattern USERNAME_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_]+$");

    @Override
    public void createMerchant(MerchantInfo merchant) {
        if (!USERNAME_PATTERN.matcher(merchant.getUsername()).matches()) {
            throw new BusinessException("用户名只能包含字母、数字和下划线，不能有中文或特殊字符");
        }
        Long exists = merchantMapper.selectCount(
                new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getUsername, merchant.getUsername()));
        if (exists > 0) throw new BusinessException("用户名已存在");
        merchant.setPassword(PasswordUtil.encode(merchant.getPassword()));
        if (merchant.getStatus() == null) merchant.setStatus(Constants.MERCHANT_STATUS_APPROVED);
        if (merchant.getScore() == null) merchant.setScore(java.math.BigDecimal.valueOf(5.0));
        merchantMapper.insert(merchant);
        log.info("Admin created merchant: {}", merchant.getUsername());
    }

    @Override
    public void updateMerchant(MerchantInfo merchant) {
        MerchantInfo existing = merchantMapper.selectById(merchant.getId());
        if (existing == null) throw new BusinessException("商户不存在");
        if (StringUtils.hasText(merchant.getPassword())) {
            merchant.setPassword(PasswordUtil.encode(merchant.getPassword()));
        } else {
            merchant.setPassword(existing.getPassword());
        }
        merchantMapper.updateById(merchant);
        log.info("Admin updated merchant: id={}", merchant.getId());
    }

    @Override
    public IPage<PurchaserInfo> purchaserPage(int current, int size, String keyword) {
        LambdaQueryWrapper<PurchaserInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(PurchaserInfo::getUsername, keyword)
                    .or().like(PurchaserInfo::getNickname, keyword);
        }
        wrapper.orderByDesc(PurchaserInfo::getCreatedTime);
        IPage<PurchaserInfo> result = purchaserMapper.selectPage(new Page<>(current, size), wrapper);
        result.getRecords().forEach(p -> p.setPassword(null));
        return result;
    }

    @Override
    public void updatePurchaserStatus(Long id, Integer status) {
        PurchaserInfo purchaser = purchaserMapper.selectById(id);
        if (purchaser == null) throw new BusinessException("采购商不存在，ID: " + id);
        purchaser.setStatus(status);
        purchaserMapper.updateById(purchaser);
        log.info("Purchaser status updated: id={}, status={}", id, status);
    }

    @Override
    public void createPurchaser(PurchaserInfo purchaser) {
        if (!USERNAME_PATTERN.matcher(purchaser.getUsername()).matches()) {
            throw new BusinessException("用户名只能包含字母、数字和下划线，不能有中文或特殊字符");
        }
        Long exists = purchaserMapper.selectCount(
                new LambdaQueryWrapper<PurchaserInfo>().eq(PurchaserInfo::getUsername, purchaser.getUsername()));
        if (exists > 0) throw new BusinessException("用户名已存在");
        purchaser.setPassword(PasswordUtil.encode(purchaser.getPassword()));
        if (purchaser.getStatus() == null) purchaser.setStatus(1);
        purchaserMapper.insert(purchaser);
        log.info("Admin created purchaser: {}", purchaser.getUsername());
    }

    @Override
    public void updatePurchaser(PurchaserInfo purchaser) {
        PurchaserInfo existing = purchaserMapper.selectById(purchaser.getId());
        if (existing == null) throw new BusinessException("采购商不存在");
        if (StringUtils.hasText(purchaser.getPassword())) {
            purchaser.setPassword(PasswordUtil.encode(purchaser.getPassword()));
        } else {
            purchaser.setPassword(existing.getPassword());
        }
        purchaserMapper.updateById(purchaser);
        log.info("Admin updated purchaser: id={}", purchaser.getId());
    }

    @Override
    public void updateMerchantStatus(Long id, Integer status) {
        MerchantInfo merchant = merchantMapper.selectById(id);
        if (merchant == null) throw new BusinessException("商户不存在，ID: " + id);
        merchant.setStatus(status);
        merchantMapper.updateById(merchant);
        log.info("Merchant status updated: id={}, status={}", id, status);
    }

    @Override
    public IPage<MedicineCategory> categoryPage(int current, int size) {
        return categoryMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<MedicineCategory>().orderByAsc(MedicineCategory::getSortOrder));
    }

    @Override
    public void createCategory(MedicineCategory category) {
        categoryMapper.insert(category);
        log.info("Category created: {}", category.getName());
    }

    @Override
    public void updateCategory(MedicineCategory category) {
        categoryMapper.updateById(category);
        log.info("Category updated: id={}", category.getId());
    }

    @Override
    public void deleteCategory(Long id) {
        Long count = medicineMapper.selectCount(
                new LambdaQueryWrapper<MedicineInfo>().eq(MedicineInfo::getCategoryId, id));
        if (count > 0) throw new BusinessException("该分类下存在药材，无法删除");
        categoryMapper.deleteById(id);
        log.info("Category deleted: id={}", id);
    }

    @Override
    public List<MedicineCategory> allCategories() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<MedicineCategory>()
                        .eq(MedicineCategory::getStatus, 1)
                        .orderByAsc(MedicineCategory::getSortOrder));
    }

    @Override
    public List<BannerInfo> bannerList() {
        return bannerMapper.selectList(
                new LambdaQueryWrapper<BannerInfo>().orderByAsc(BannerInfo::getSortOrder));
    }

    @Override
    public void createBanner(BannerInfo banner) {
        bannerMapper.insert(banner);
        log.info("Banner created: {}", banner.getTitle());
    }

    @Override
    public void updateBanner(BannerInfo banner) {
        bannerMapper.updateById(banner);
        log.info("Banner updated: id={}", banner.getId());
    }

    @Override
    public void deleteBanner(Long id) {
        bannerMapper.deleteById(id);
        log.info("Banner deleted: id={}", id);
    }

    @Override
    public IPage<AnnouncementInfo> announcementPage(int current, int size) {
        return announcementMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<AnnouncementInfo>().orderByDesc(AnnouncementInfo::getCreatedTime));
    }

    @Override
    public void createAnnouncement(AnnouncementInfo announcement) {
        announcementMapper.insert(announcement);
        if (announcement.getStatus() != null && announcement.getStatus() == 1) {
            notifyMerchantsOfAnnouncement(announcement);
        }
        log.info("Announcement created: {}", announcement.getTitle());
    }

    @Override
    public void updateAnnouncement(AnnouncementInfo announcement) {
        announcementMapper.updateById(announcement);
    }

    private void notifyMerchantsOfAnnouncement(AnnouncementInfo announcement) {
        try {
            List<MerchantInfo> merchants = merchantMapper.selectList(
                    new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getStatus, Constants.MERCHANT_STATUS_APPROVED));
            for (MerchantInfo m : merchants) {
                MerchantNotification n = new MerchantNotification();
                n.setMerchantId(m.getId());
                n.setType(5);
                n.setTitle("系统公告");
                n.setContent(announcement.getTitle() + (announcement.getContent() != null && announcement.getContent().length() > 50
                        ? "：" + announcement.getContent().substring(0, 50) + "..." : ""));
                n.setExtraData("{\"announcementId\":" + announcement.getId() + "}");
                n.setIsRead(0);
                merchantNotificationMapper.insert(n);
            }
        } catch (Exception e) {
            log.warn("Failed to notify merchants of announcement: {}", e.getMessage());
        }
    }

    @Override
    public void deleteAnnouncement(Long id) {
        announcementMapper.deleteById(id);
        log.info("Announcement deleted: id={}", id);
    }

    @Override
    public List<SystemConfig> configList() {
        return systemConfigMapper.selectList(null);
    }

    @Override
    public void updateConfig(SystemConfig config) {
        systemConfigMapper.updateById(config);
        log.info("Config updated: key={}", config.getConfigKey());
    }

    @Override
    public List<LowStockVO> lowStockList() {
        List<InventoryInfo> list = inventoryMapper.selectLowStockInventories();
        List<LowStockVO> result = new java.util.ArrayList<>();
        for (InventoryInfo inv : list) {
            MedicineInfo medicine = medicineMapper.selectById(inv.getMedicineId());
            MerchantInfo merchant = merchantMapper.selectById(inv.getMerchantId());
            MedicineCategory category = medicine != null ? categoryMapper.selectById(medicine.getCategoryId()) : null;
            LowStockVO vo = new LowStockVO();
            vo.setId(inv.getId());
            vo.setMedicineId(inv.getMedicineId());
            vo.setMerchantId(inv.getMerchantId());
            vo.setStockQuantity(inv.getStockQuantity());
            vo.setWarningThreshold(inv.getWarningThreshold());
            vo.setName(medicine != null ? medicine.getName() : null);
            vo.setPrice(medicine != null ? medicine.getPrice() : null);
            vo.setUnit(medicine != null ? medicine.getUnit() : null);
            vo.setMerchantName(merchant != null ? merchant.getCompanyName() : null);
            vo.setCategoryName(category != null ? category.getName() : null);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<PriceRecord> priceHistory(Long medicineId) {
        return priceRecordMapper.selectList(
                new LambdaQueryWrapper<PriceRecord>()
                        .eq(PriceRecord::getMedicineId, medicineId)
                        .orderByAsc(PriceRecord::getRecordTime));
    }

    @Override
    public IPage<FeedbackInfo> feedbackPage(int current, int size, Integer status, Integer type) {
        LambdaQueryWrapper<FeedbackInfo> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(FeedbackInfo::getStatus, status);
        if (type != null) wrapper.eq(FeedbackInfo::getType, type);
        wrapper.orderByDesc(FeedbackInfo::getCreatedTime);
        return feedbackMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public void replyFeedback(Long id, String reply) {
        FeedbackInfo f = feedbackMapper.selectById(id);
        if (f == null) throw new BusinessException("反馈不存在");
        f.setReply(reply);
        f.setReplyTime(java.time.LocalDateTime.now());
        f.setStatus(1);
        feedbackMapper.updateById(f);
        log.info("Feedback replied: id={}", id);
    }

    @Override
    public void closeFeedback(Long id) {
        FeedbackInfo f = feedbackMapper.selectById(id);
        if (f == null) throw new BusinessException("反馈不存在");
        f.setStatus(2);
        feedbackMapper.updateById(f);
        log.info("Feedback closed: id={}", id);
    }

    @Override
    public IPage<OperationLog> operationLogPage(int current, int size, String module, String keyword) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) wrapper.eq(OperationLog::getModule, module);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OperationLog::getOperatorName, keyword)
                    .or().like(OperationLog::getAction, keyword)
                    .or().like(OperationLog::getDetail, keyword));
        }
        wrapper.orderByDesc(OperationLog::getCreatedTime);
        return operationLogMapper.selectPage(new Page<>(current, size), wrapper);
    }
}
