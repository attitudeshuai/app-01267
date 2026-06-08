package com.medicine.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medicine.sales.vo.MedicineVO;

public interface CollectionService {
    IPage<MedicineVO> page(int current, int size, Long purchaserId);
    void toggle(Long purchaserId, Long medicineId);
    boolean isCollected(Long purchaserId, Long medicineId);
}
