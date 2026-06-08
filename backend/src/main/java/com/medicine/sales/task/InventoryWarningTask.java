package com.medicine.sales.task;

import com.medicine.sales.entity.InventoryInfo;
import com.medicine.sales.entity.MedicineInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.sales.entity.MerchantNotification;
import com.medicine.sales.mapper.InventoryMapper;
import com.medicine.sales.mapper.MedicineMapper;
import com.medicine.sales.mapper.MerchantNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson.JSON;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InventoryWarningTask {

    private static final String REDIS_WARNING_PREFIX = "inventory:warning:";

    @Resource
    private InventoryMapper inventoryMapper;

    @Resource
    private MedicineMapper medicineMapper;

    @Resource
    private MerchantNotificationMapper notificationMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 3600000)
    public void checkLowStock() {
        log.info("开始检查库存预警...");
        List<InventoryInfo> lowStockList = inventoryMapper.selectLowStockInventories();

        if (lowStockList.isEmpty()) {
            log.info("库存检查完成，无预警项");
            return;
        }

        log.warn("库存预警: {} 件药材低于阈值", lowStockList.size());

        Map<Long, List<InventoryInfo>> merchantInventoryMap = lowStockList.stream()
                .collect(Collectors.groupingBy(InventoryInfo::getMerchantId));

        for (Map.Entry<Long, List<InventoryInfo>> entry : merchantInventoryMap.entrySet()) {
            Long merchantId = entry.getKey();
            List<InventoryInfo> inventories = entry.getValue();

            Map<String, Object> warningData = new HashMap<>();
            warningData.put("merchantId", merchantId);
            warningData.put("warningCount", inventories.size());
            warningData.put("checkTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            List<Map<String, Object>> items = inventories.stream().map(inv -> {
                Map<String, Object> item = new HashMap<>();
                item.put("medicineId", inv.getMedicineId());
                item.put("currentStock", inv.getStockQuantity());
                item.put("threshold", inv.getWarningThreshold());

                MedicineInfo medicine = medicineMapper.selectById(inv.getMedicineId());
                if (medicine != null) {
                    item.put("medicineName", medicine.getName());
                }
                return item;
            }).collect(Collectors.toList());

            warningData.put("items", items);

            String redisKey = REDIS_WARNING_PREFIX + merchantId;
            redisTemplate.opsForValue().set(redisKey, warningData, 24, TimeUnit.HOURS);

            long recentCount = notificationMapper.selectCount(
                    new LambdaQueryWrapper<MerchantNotification>()
                            .eq(MerchantNotification::getMerchantId, merchantId)
                            .eq(MerchantNotification::getType, 1)
                            .eq(MerchantNotification::getIsRead, 0)
                            .ge(MerchantNotification::getCreatedTime, LocalDateTime.now().minusHours(6)));
            if (recentCount == 0) {
                MerchantNotification notification = new MerchantNotification();
                notification.setMerchantId(merchantId);
                notification.setType(1);
                notification.setTitle("库存预警通知");
                notification.setContent(String.format("您有%d件药材库存低于预警阈值，请及时补货", inventories.size()));
                notification.setExtraData(JSON.toJSONString(warningData));
                notification.setIsRead(0);
                notificationMapper.insert(notification);
            }

            log.warn("商户[{}]有{}件药材库存不足:", merchantId, inventories.size());
            for (InventoryInfo inv : inventories) {
                log.warn("  - 药材ID={}, 当前库存={}, 预警阈值={}",
                        inv.getMedicineId(), inv.getStockQuantity(), inv.getWarningThreshold());
            }
        }

        log.info("库存预警检查完成，共{}个商户收到预警通知", merchantInventoryMap.size());
    }
}
