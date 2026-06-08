package com.medicine.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.sales.common.Constants;
import com.medicine.sales.dto.MedicineDTO;
import com.medicine.sales.entity.InventoryInfo;
import com.medicine.sales.entity.MedicineInfo;
import com.medicine.sales.entity.MerchantNotification;
import com.medicine.sales.entity.PriceRecord;
import com.medicine.sales.entity.RecommendRecord;
import com.medicine.sales.exception.BusinessException;
import com.medicine.sales.mapper.*;
import com.medicine.sales.entity.SystemConfig;
import com.medicine.sales.service.MedicineService;
import com.medicine.sales.vo.MedicineVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MedicineServiceImpl implements MedicineService {

    @Resource
    private MedicineMapper medicineMapper;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private InventoryMapper inventoryMapper;
    @Resource
    private PriceRecordMapper priceRecordMapper;
    @Resource
    private SystemConfigMapper systemConfigMapper;
    @Resource
    private MerchantNotificationMapper notificationMapper;
    @Resource
    private RecommendRecordMapper recommendRecordMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public IPage<MedicineVO> page(int current, int size, String keyword, Long categoryId, Long merchantId, Integer status) {
        Page<MedicineVO> page = new Page<>(current, size);
        return medicineMapper.selectMedicinePage(page, keyword, categoryId, merchantId, status);
    }

    @Override
    public MedicineVO getDetail(Long id) {
        String cacheKey = Constants.REDIS_MEDICINE_PREFIX + id;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof MedicineVO) {
                return (MedicineVO) cached;
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for medicine {}: {}, clearing", id, e.getMessage());
            try { redisTemplate.delete(cacheKey); } catch (Exception ignored) {}
        }

        MedicineInfo info = medicineMapper.selectById(id);
        if (info == null) {
            throw new BusinessException("药材不存在，ID: " + id);
        }

        MedicineVO vo = new MedicineVO();
        BeanUtils.copyProperties(info, vo);

        InventoryInfo inv = inventoryMapper.selectOne(
                new LambdaQueryWrapper<InventoryInfo>()
                        .eq(InventoryInfo::getMedicineId, id)
                        .eq(InventoryInfo::getMerchantId, info.getMerchantId()));
        if (inv != null) {
            vo.setStockQuantity(inv.getStockQuantity());
            vo.setWarningThreshold(inv.getWarningThreshold());
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, vo, 2, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis cache write failed for medicine {}: {}", id, e.getMessage());
        }
        return vo;
    }

    @Override
    @Transactional
    public void create(MedicineDTO dto, Long merchantId) {
        MedicineInfo medicine = new MedicineInfo();
        BeanUtils.copyProperties(dto, medicine);
        medicine.setMerchantId(merchantId);
        medicine.setStatus(Constants.MEDICINE_STATUS_PENDING);
        medicine.setSalesCount(0);
        if (medicine.getTraceCode() == null || medicine.getTraceCode().trim().isEmpty()) {
            medicine.setTraceCode("TC" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        }
        medicineMapper.insert(medicine);

        InventoryInfo inventory = new InventoryInfo();
        inventory.setMedicineId(medicine.getId());
        inventory.setMerchantId(merchantId);
        inventory.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);
        inventory.setWarningThreshold(dto.getWarningThreshold() != null ? dto.getWarningThreshold() : 10);
        inventoryMapper.insert(inventory);

        PriceRecord priceRecord = new PriceRecord();
        priceRecord.setMedicineId(medicine.getId());
        priceRecord.setMerchantId(merchantId);
        priceRecord.setPrice(dto.getPrice());
        priceRecord.setIsAbnormal(0);
        priceRecord.setRecordTime(LocalDateTime.now());
        priceRecordMapper.insert(priceRecord);

        log.info("Medicine created: id={}, name={}, merchantId={}", medicine.getId(), medicine.getName(), merchantId);
    }

    @Override
    @Transactional
    public void update(MedicineDTO dto, Long merchantId) {
        MedicineInfo existing = medicineMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("药材不存在，ID: " + dto.getId());
        }
        if (merchantId != null && !existing.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权限更新此药材，药材ID: " + dto.getId());
        }

        BigDecimal oldPrice = existing.getPrice();
        BeanUtils.copyProperties(dto, existing, "id", "merchantId", "status", "salesCount", "createdTime");
        medicineMapper.updateById(existing);

        if (dto.getStockQuantity() != null) {
            InventoryInfo inv = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<InventoryInfo>()
                            .eq(InventoryInfo::getMedicineId, existing.getId())
                            .eq(InventoryInfo::getMerchantId, existing.getMerchantId()));
            if (inv != null) {
                inv.setStockQuantity(dto.getStockQuantity());
                if (dto.getWarningThreshold() != null) {
                    inv.setWarningThreshold(dto.getWarningThreshold());
                }
                inventoryMapper.updateById(inv);
            }
        }

        if (dto.getPrice() != null && oldPrice != null && dto.getPrice().compareTo(oldPrice) != 0) {
            boolean isAbnormal = isPriceAbnormal(oldPrice, dto.getPrice());
            PriceRecord priceRecord = new PriceRecord();
            priceRecord.setMedicineId(existing.getId());
            priceRecord.setMerchantId(existing.getMerchantId());
            priceRecord.setPrice(dto.getPrice());
            priceRecord.setIsAbnormal(isAbnormal ? 1 : 0);
            priceRecord.setRecordTime(LocalDateTime.now());
            priceRecordMapper.insert(priceRecord);
            if (isAbnormal) {
                notifyPriceAbnormal(existing.getMerchantId(), existing.getName(), oldPrice, dto.getPrice());
            }
        }

        redisTemplate.delete(Constants.REDIS_MEDICINE_PREFIX + existing.getId());
        log.info("Medicine updated: id={}", dto.getId());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        MedicineInfo medicine = medicineMapper.selectById(id);
        if (medicine == null) {
            throw new BusinessException("药材不存在，ID: " + id);
        }
        Integer oldStatus = medicine.getStatus();
        medicine.setStatus(status);
        medicineMapper.updateById(medicine);
        redisTemplate.delete(Constants.REDIS_MEDICINE_PREFIX + id);
        if (oldStatus != null && oldStatus == Constants.MEDICINE_STATUS_PENDING && (status == Constants.MEDICINE_STATUS_ON_SHELF || status == Constants.MEDICINE_STATUS_REJECTED)) {
            MerchantNotification notif = new MerchantNotification();
            notif.setMerchantId(medicine.getMerchantId());
            notif.setType(4);
            notif.setTitle(status == Constants.MEDICINE_STATUS_ON_SHELF ? "药材审核通过" : "药材审核未通过");
            notif.setContent(String.format("您提交的药材「%s」%s", medicine.getName(),
                    status == Constants.MEDICINE_STATUS_ON_SHELF ? "已审核通过并上架" : "审核未通过"));
            notif.setExtraData("{\"medicineId\":" + id + ",\"medicineName\":\"" + medicine.getName() + "\"}");
            notif.setIsRead(0);
            try { notificationMapper.insert(notif); } catch (Exception e) { log.warn("Failed to create audit notification", e); }
        }
        log.info("Medicine status updated: id={}, status={}", id, status);
    }

    @Override
    public void delete(Long id, Long merchantId) {
        MedicineInfo medicine = medicineMapper.selectById(id);
        if (medicine == null) {
            throw new BusinessException("药材不存在，ID: " + id);
        }
        if (merchantId != null && !medicine.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权限删除此药材，药材ID: " + id);
        }
        medicineMapper.deleteById(id);
        inventoryMapper.delete(new LambdaQueryWrapper<InventoryInfo>().eq(InventoryInfo::getMedicineId, id));
        redisTemplate.delete(Constants.REDIS_MEDICINE_PREFIX + id);
        log.info("Medicine deleted: id={}", id);
    }

    @Override
    @Transactional
    public void adminCreate(MedicineInfo medicine) {
        if (medicine.getStatus() == null) {
            medicine.setStatus(Constants.MEDICINE_STATUS_ON_SHELF);
        }
        if (medicine.getSalesCount() == null) {
            medicine.setSalesCount(0);
        }
        if (medicine.getTraceCode() == null || medicine.getTraceCode().trim().isEmpty()) {
            medicine.setTraceCode("TC" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        }
        medicineMapper.insert(medicine);

        int defaultStock = medicine.getStockQuantity() != null ? medicine.getStockQuantity() : getConfigInt("default_stock_quantity", 500);
        int warningThreshold = medicine.getWarningThreshold() != null ? medicine.getWarningThreshold() : getConfigInt("inventory_warning_threshold", 50);
        InventoryInfo inventory = new InventoryInfo();
        inventory.setMedicineId(medicine.getId());
        inventory.setMerchantId(medicine.getMerchantId());
        inventory.setStockQuantity(defaultStock);
        inventory.setWarningThreshold(warningThreshold);
        inventoryMapper.insert(inventory);

        PriceRecord priceRecord = new PriceRecord();
        priceRecord.setMedicineId(medicine.getId());
        priceRecord.setMerchantId(medicine.getMerchantId());
        priceRecord.setPrice(medicine.getPrice());
        priceRecord.setIsAbnormal(0);
        priceRecord.setRecordTime(LocalDateTime.now());
        priceRecordMapper.insert(priceRecord);

        log.info("Admin created medicine: id={}, name={}", medicine.getId(), medicine.getName());
    }

    @Override
    @Transactional
    public void adminUpdate(MedicineInfo medicine) {
        MedicineInfo existing = medicineMapper.selectById(medicine.getId());
        if (existing == null) {
            throw new BusinessException("药材不存在，ID: " + medicine.getId());
        }

        if (medicine.getPrice() != null && existing.getPrice() != null 
                && medicine.getPrice().compareTo(existing.getPrice()) != 0) {
            BigDecimal oldPrice = existing.getPrice();
            boolean isAbnormal = isPriceAbnormal(oldPrice, medicine.getPrice());
            PriceRecord priceRecord = new PriceRecord();
            priceRecord.setMedicineId(existing.getId());
            priceRecord.setMerchantId(existing.getMerchantId());
            priceRecord.setPrice(medicine.getPrice());
            priceRecord.setIsAbnormal(isAbnormal ? 1 : 0);
            priceRecord.setRecordTime(LocalDateTime.now());
            priceRecordMapper.insert(priceRecord);
            if (isAbnormal) {
                notifyPriceAbnormal(existing.getMerchantId(), existing.getName(), oldPrice, medicine.getPrice());
            }
        }

        medicineMapper.updateById(medicine);

        if (medicine.getStockQuantity() != null || medicine.getWarningThreshold() != null) {
            Long targetMerchantId = medicine.getMerchantId() != null ? medicine.getMerchantId() : existing.getMerchantId();
            InventoryInfo inv = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<InventoryInfo>()
                            .eq(InventoryInfo::getMedicineId, existing.getId())
                            .eq(InventoryInfo::getMerchantId, targetMerchantId));
            if (inv != null) {
                if (medicine.getStockQuantity() != null) inv.setStockQuantity(medicine.getStockQuantity());
                if (medicine.getWarningThreshold() != null) inv.setWarningThreshold(medicine.getWarningThreshold());
                inventoryMapper.updateById(inv);
            }
        }

        redisTemplate.delete(Constants.REDIS_MEDICINE_PREFIX + medicine.getId());
        log.info("Admin updated medicine: id={}", medicine.getId());
    }

    @Override
    @Transactional
    public void adminDelete(Long id) {
        MedicineInfo medicine = medicineMapper.selectById(id);
        if (medicine == null) {
            throw new BusinessException("药材不存在，ID: " + id);
        }
        medicineMapper.deleteById(id);
        inventoryMapper.delete(new LambdaQueryWrapper<InventoryInfo>().eq(InventoryInfo::getMedicineId, id));
        redisTemplate.delete(Constants.REDIS_MEDICINE_PREFIX + id);
        log.info("Admin deleted medicine: id={}", id);
    }

    @Override
    public List<MedicineVO> getRecommendList(Long purchaserId) {
        return getRecommendList(purchaserId, false);
    }

    @Override
    public List<MedicineVO> getRecommendList(Long purchaserId, boolean forceRefresh) {
        int similarCount = getConfigInt("recommend_similar_count", 10);
        int itemCount = getConfigInt("recommend_item_count", 10);

        String cacheKey = Constants.REDIS_RECOMMEND_PREFIX + purchaserId;
        Object cached = forceRefresh ? null : redisTemplate.opsForValue().get(cacheKey);
        List<MedicineVO> result = null;
        boolean computedFresh = false;

        if (cached instanceof List) {
            result = (List<MedicineVO>) cached;
        } else {
            computedFresh = true;
            List<Long> purchasedIds = orderMapper.getPurchaseMedicineIds(purchaserId);
            if (purchasedIds.isEmpty()) {
                result = getHotList();
            } else {
                Map<Long, Double> userSimilarity = calculateUserSimilarity(purchaserId);
                result = generateRecommendListFromSimilarity(purchaserId, purchasedIds, userSimilarity, similarCount, itemCount);
                if (result == null || result.isEmpty()) {
                    result = getRecommendByCategory(purchasedIds, itemCount);
                }
            }
            if (result == null || result.isEmpty()) {
                result = getHotList();
            }
            int cacheHours = getConfigInt("recommend_cache_hours", 1);
            if (result != null && !result.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, result, cacheHours, TimeUnit.HOURS);
            }
        }

        if (computedFresh && result != null && !result.isEmpty()) {
            saveRecommendRecords(purchaserId, result.stream().map(MedicineVO::getId).collect(Collectors.toList()));
        }
        return result != null ? result : getHotList();
    }

    private List<MedicineVO> getRecommendByCategory(List<Long> purchasedIds, int limit) {
        if (purchasedIds == null || purchasedIds.isEmpty()) return Collections.emptyList();
        List<MedicineInfo> purchased = medicineMapper.selectBatchIds(purchasedIds);
        Set<Long> categoryIds = purchased.stream().map(MedicineInfo::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (categoryIds.isEmpty()) return Collections.emptyList();
        List<MedicineInfo> medicines = medicineMapper.selectList(
                new LambdaQueryWrapper<MedicineInfo>()
                        .in(MedicineInfo::getCategoryId, categoryIds)
                        .notIn(MedicineInfo::getId, purchasedIds)
                        .eq(MedicineInfo::getStatus, Constants.MEDICINE_STATUS_ON_SHELF)
                        .orderByDesc(MedicineInfo::getSalesCount)
                        .last("LIMIT " + limit));
        return medicines.stream().map(m -> {
            MedicineVO vo = new MedicineVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    private void saveRecommendRecords(Long purchaserId, List<Long> medicineIds) {
        try {
            LocalDateTime now = LocalDateTime.now();
            for (Long medicineId : medicineIds) {
                RecommendRecord record = new RecommendRecord();
                record.setPurchaserId(purchaserId);
                record.setMedicineId(medicineId);
                record.setRecommendTime(now);
                record.setIsClicked(0);
                record.setIsOrdered(0);
                recommendRecordMapper.insert(record);
            }
        } catch (Exception e) {
            log.warn("保存推荐记录失败: purchaserId={}", purchaserId, e);
        }
    }

    @Override
    public void recordRecommendClick(Long purchaserId, Long medicineId) {
        try {
            RecommendRecord record = recommendRecordMapper.selectOne(
                    new LambdaQueryWrapper<RecommendRecord>()
                            .eq(RecommendRecord::getPurchaserId, purchaserId)
                            .eq(RecommendRecord::getMedicineId, medicineId)
                            .eq(RecommendRecord::getIsClicked, 0)
                            .orderByDesc(RecommendRecord::getRecommendTime)
                            .last("LIMIT 1"));
            if (record != null) {
                record.setIsClicked(1);
                recommendRecordMapper.updateById(record);
            }
        } catch (Exception e) {
            log.warn("记录推荐点击失败: purchaserId={}, medicineId={}", purchaserId, medicineId, e);
        }
    }

    @Override
    public void recordRecommendOrdered(Long purchaserId, List<Long> medicineIds) {
        if (medicineIds == null || medicineIds.isEmpty()) return;
        try {
            for (Long medicineId : medicineIds) {
                RecommendRecord record = recommendRecordMapper.selectOne(
                        new LambdaQueryWrapper<RecommendRecord>()
                                .eq(RecommendRecord::getPurchaserId, purchaserId)
                                .eq(RecommendRecord::getMedicineId, medicineId)
                                .eq(RecommendRecord::getIsOrdered, 0)
                                .orderByDesc(RecommendRecord::getRecommendTime)
                                .last("LIMIT 1"));
                if (record != null) {
                    record.setIsOrdered(1);
                    recommendRecordMapper.updateById(record);
                }
            }
        } catch (Exception e) {
            log.warn("记录推荐下单失败: purchaserId={}, medicineIds={}", purchaserId, medicineIds, e);
        }
    }

    @Override
    public List<MedicineVO> getHotList() {
        Object cached = redisTemplate.opsForValue().get(Constants.REDIS_HOT_MEDICINE);
        if (cached instanceof List) {
            return (List<MedicineVO>) cached;
        }

        List<MedicineInfo> medicines = medicineMapper.selectList(
                new LambdaQueryWrapper<MedicineInfo>()
                        .eq(MedicineInfo::getStatus, Constants.MEDICINE_STATUS_ON_SHELF)
                        .orderByDesc(MedicineInfo::getSalesCount)
                        .last("LIMIT 10"));

        List<MedicineVO> result = medicines.stream().map(m -> {
            MedicineVO vo = new MedicineVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());

        redisTemplate.opsForValue().set(Constants.REDIS_HOT_MEDICINE, result, 1, TimeUnit.HOURS);
        return result;
    }

    /**
     * 计算用户相似度（与 README 五.2 采购推荐模块设计一致）
     * 1. 获取当前用户历史采购记录 2. 匹配有相似采购记录的用户 3. 计算余弦相似度，返回用户ID与相似度映射
     */
    private Map<Long, Double> calculateUserSimilarity(Long purchaserId) {
        List<Long> purchaseMedicineIds = orderMapper.getPurchaseMedicineIds(purchaserId);
        if (purchaseMedicineIds.isEmpty()) return Collections.emptyMap();
        List<Long> similarPurchasers = orderMapper.getSimilarPurchasers(purchaseMedicineIds, purchaserId);
        Map<Long, Double> similarityMap = new HashMap<>();
        for (Long similarPurchaser : similarPurchasers) {
            List<Long> similarPurchaseIds = orderMapper.getPurchaseMedicineIds(similarPurchaser);
            double similarity = cosineSimilarity(purchaseMedicineIds, similarPurchaseIds);
            similarityMap.put(similarPurchaser, similarity);
        }
        return similarityMap;
    }

    /**
     * 根据相似度映射生成推荐列表（与 README Prompt 中 generateRecommendList 一致）
     * 筛选高相似度用户，获取其采购的药材，过滤当前用户已采购的药材，按销量排序
     */
    private List<MedicineVO> generateRecommendListFromSimilarity(Long purchaserId, List<Long> purchasedIds,
                                                                 Map<Long, Double> userSimilarity, int similarCount, int itemCount) {
        if (userSimilarity.isEmpty()) return null;
        // 筛选高相似度用户，获取其采购的药材（对应 Prompt: orderMapper.getHighSimilarityPurchaseMedicines）
        List<Long> topPurchaserIds = userSimilarity.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(similarCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<Long> recommendMedicineIds = orderMapper.getHighSimilarityPurchaseMedicines(topPurchaserIds);
        if (recommendMedicineIds == null || recommendMedicineIds.isEmpty()) return null;
        // 过滤当前用户已采购的药材，按销量排序（对应 Prompt: medicineMapper.getRecommendMedicines）
        return medicineMapper.getRecommendMedicines(recommendMedicineIds, purchaserId, itemCount);
    }

    /**
     * 计算两个采购记录集合的余弦相似度（作为二值向量，1=已购买 0=未购买）
     * cos = (A·B) / (||A|| * ||B||)，使用 Set 去重保证集合语义，避免 List 重复元素导致计算不准确
     */
    private double cosineSimilarity(List<Long> list1, List<Long> list2) {
        Set<Long> set1 = new HashSet<>(list1);
        Set<Long> set2 = new HashSet<>(list2);
        if (set1.isEmpty() || set2.isEmpty()) return 0;

        int dotProduct = 0;
        for (Long id : set1) {
            if (set2.contains(id)) dotProduct++;
        }

        double norm1 = Math.sqrt(set1.size());
        double norm2 = Math.sqrt(set2.size());
        if (norm1 == 0 || norm2 == 0) return 0;
        return dotProduct / (norm1 * norm2);
    }

    private int getConfigInt(String key, int defaultValue) {
        try {
            SystemConfig config = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
            return config != null && config.getConfigValue() != null
                    ? Integer.parseInt(config.getConfigValue().trim()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double getConfigDouble(String key, double defaultValue) {
        try {
            SystemConfig config = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
            return config != null && config.getConfigValue() != null
                    ? Double.parseDouble(config.getConfigValue().trim()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean isPriceAbnormal(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice == null || newPrice == null || oldPrice.compareTo(BigDecimal.ZERO) == 0) return false;
        double rate = getConfigDouble("price_abnormal_rate", 0.3);
        BigDecimal change = newPrice.subtract(oldPrice).abs();
        BigDecimal threshold = oldPrice.multiply(BigDecimal.valueOf(rate)).setScale(4, RoundingMode.HALF_UP);
        return change.compareTo(threshold) > 0;
    }

    private void notifyPriceAbnormal(Long merchantId, String medicineName, BigDecimal oldPrice, BigDecimal newPrice) {
        try {
            MerchantNotification n = new MerchantNotification();
            n.setMerchantId(merchantId);
            n.setType(2);
            n.setTitle("价格异常波动预警");
            n.setContent(String.format("药材「%s」价格波动较大：原价¥%s → 现价¥%s，请核实", 
                    medicineName, oldPrice, newPrice));
            n.setIsRead(0);
            notificationMapper.insert(n);
        } catch (Exception e) {
            log.warn("Failed to create price abnormal notification", e);
        }
    }
}
