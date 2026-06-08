package com.medicine.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medicine.sales.entity.*;
import com.medicine.sales.vo.DashboardVO;
import com.medicine.sales.vo.LowStockVO;

import java.util.List;

public interface AdminService {
    DashboardVO getDashboard();
    IPage<MerchantInfo> merchantPage(int current, int size, Integer status, String keyword);
    void auditMerchant(Long merchantId, Integer status);
    void createMerchant(MerchantInfo merchant);
    void updateMerchant(MerchantInfo merchant);
    IPage<PurchaserInfo> purchaserPage(int current, int size, String keyword);
    void updatePurchaserStatus(Long id, Integer status);
    void createPurchaser(PurchaserInfo purchaser);
    void updatePurchaser(PurchaserInfo purchaser);
    void updateMerchantStatus(Long id, Integer status);
    IPage<MedicineCategory> categoryPage(int current, int size);
    void createCategory(MedicineCategory category);
    void updateCategory(MedicineCategory category);
    void deleteCategory(Long id);
    List<MedicineCategory> allCategories();
    List<BannerInfo> bannerList();
    void createBanner(BannerInfo banner);
    void updateBanner(BannerInfo banner);
    void deleteBanner(Long id);
    IPage<AnnouncementInfo> announcementPage(int current, int size);
    void createAnnouncement(AnnouncementInfo announcement);
    void updateAnnouncement(AnnouncementInfo announcement);
    void deleteAnnouncement(Long id);
    List<SystemConfig> configList();
    void updateConfig(SystemConfig config);
    List<LowStockVO> lowStockList();
    List<PriceRecord> priceHistory(Long medicineId);
    IPage<FeedbackInfo> feedbackPage(int current, int size, Integer status, Integer type);
    void replyFeedback(Long id, String reply);
    void closeFeedback(Long id);
    IPage<OperationLog> operationLogPage(int current, int size, String module, String keyword);
}
