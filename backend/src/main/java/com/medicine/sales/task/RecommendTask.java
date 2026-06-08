package com.medicine.sales.task;

import com.medicine.sales.entity.PurchaserInfo;
import com.medicine.sales.mapper.PurchaserMapper;
import com.medicine.sales.service.MedicineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class RecommendTask {

    @Resource
    private PurchaserMapper purchaserMapper;
    @Resource
    private MedicineService medicineService;
    @Scheduled(fixedRate = 3600000)
    public void refreshRecommendations() {
        log.info("开始刷新推荐缓存...");
        List<PurchaserInfo> purchasers = purchaserMapper.selectList(null);
        int successCount = 0;
        for (PurchaserInfo p : purchasers) {
            try {
                medicineService.getRecommendList(p.getId(), true);
                successCount++;
            } catch (Exception e) {
                log.error("刷新采购商推荐失败: purchaserId={}", p.getId(), e);
            }
        }
        log.info("推荐缓存刷新完成: 成功{}/总共{}个采购商", successCount, purchasers.size());
    }
}
