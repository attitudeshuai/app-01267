package com.medicine.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.sales.entity.CollectionInfo;
import com.medicine.sales.entity.MedicineInfo;
import com.medicine.sales.mapper.CollectionMapper;
import com.medicine.sales.mapper.MedicineMapper;
import com.medicine.sales.service.CollectionService;
import com.medicine.sales.vo.MedicineVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CollectionServiceImpl implements CollectionService {

    @Resource
    private CollectionMapper collectionMapper;
    @Resource
    private MedicineMapper medicineMapper;

    @Override
    public IPage<MedicineVO> page(int current, int size, Long purchaserId) {
        Page<CollectionInfo> collPage = new Page<>(current, size);
        IPage<CollectionInfo> collResult = collectionMapper.selectPage(collPage,
                new LambdaQueryWrapper<CollectionInfo>()
                        .eq(CollectionInfo::getPurchaserId, purchaserId)
                        .orderByDesc(CollectionInfo::getCreatedTime));

        Page<MedicineVO> result = new Page<>(current, size, collResult.getTotal());
        List<Long> medicineIds = collResult.getRecords().stream()
                .map(CollectionInfo::getMedicineId).collect(Collectors.toList());

        if (!medicineIds.isEmpty()) {
            List<MedicineInfo> medicines = medicineMapper.selectBatchIds(medicineIds);
            List<MedicineVO> vos = medicines.stream().map(m -> {
                MedicineVO vo = new MedicineVO();
                BeanUtils.copyProperties(m, vo);
                vo.setCollected(true);
                return vo;
            }).collect(Collectors.toList());
            result.setRecords(vos);
        }
        return result;
    }

    @Override
    public void toggle(Long purchaserId, Long medicineId) {
        CollectionInfo existing = collectionMapper.selectOne(
                new LambdaQueryWrapper<CollectionInfo>()
                        .eq(CollectionInfo::getPurchaserId, purchaserId)
                        .eq(CollectionInfo::getMedicineId, medicineId));
        if (existing != null) {
            collectionMapper.deleteById(existing.getId());
            log.info("Collection removed: purchaserId={}, medicineId={}", purchaserId, medicineId);
        } else {
            CollectionInfo collection = new CollectionInfo();
            collection.setPurchaserId(purchaserId);
            collection.setMedicineId(medicineId);
            collectionMapper.insert(collection);
            log.info("Collection added: purchaserId={}, medicineId={}", purchaserId, medicineId);
        }
    }

    @Override
    public boolean isCollected(Long purchaserId, Long medicineId) {
        return collectionMapper.selectCount(
                new LambdaQueryWrapper<CollectionInfo>()
                        .eq(CollectionInfo::getPurchaserId, purchaserId)
                        .eq(CollectionInfo::getMedicineId, medicineId)) > 0;
    }
}
