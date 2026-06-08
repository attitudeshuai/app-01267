package com.medicine.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medicine.sales.dto.MedicineDTO;
import com.medicine.sales.entity.MedicineInfo;
import com.medicine.sales.vo.MedicineVO;

import java.util.List;

public interface MedicineService {
    IPage<MedicineVO> page(int current, int size, String keyword, Long categoryId, Long merchantId, Integer status);
    MedicineVO getDetail(Long id);
    void create(MedicineDTO dto, Long merchantId);
    void update(MedicineDTO dto, Long merchantId);
    void updateStatus(Long id, Integer status);
    void delete(Long id, Long merchantId);
    void adminCreate(MedicineInfo medicine);
    void adminUpdate(MedicineInfo medicine);
    void adminDelete(Long id);
    List<MedicineVO> getRecommendList(Long purchaserId);
    List<MedicineVO> getRecommendList(Long purchaserId, boolean forceRefresh);
    List<MedicineVO> getHotList();
    void recordRecommendClick(Long purchaserId, Long medicineId);
    void recordRecommendOrdered(Long purchaserId, List<Long> medicineIds);
}
