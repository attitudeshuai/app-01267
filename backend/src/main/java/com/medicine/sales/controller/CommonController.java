package com.medicine.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.medicine.sales.common.Result;
import com.medicine.sales.entity.AnnouncementInfo;
import com.medicine.sales.entity.BannerInfo;
import com.medicine.sales.entity.MedicineCategory;
import com.medicine.sales.entity.MedicineInfo;
import com.medicine.sales.mapper.AnnouncementMapper;
import com.medicine.sales.mapper.BannerMapper;
import com.medicine.sales.mapper.MedicineCategoryMapper;
import com.medicine.sales.service.MedicineService;
import com.medicine.sales.vo.MedicineVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/common")
public class CommonController {

    @Resource
    private BannerMapper bannerMapper;
    @Resource
    private MedicineCategoryMapper categoryMapper;
    @Resource
    private AnnouncementMapper announcementMapper;
    @Resource
    private MedicineService medicineService;

    @GetMapping("/banners")
    public Result<List<BannerInfo>> banners() {
        return Result.success(bannerMapper.selectList(
                new LambdaQueryWrapper<BannerInfo>()
                        .eq(BannerInfo::getStatus, 1)
                        .orderByAsc(BannerInfo::getSortOrder)));
    }

    @GetMapping("/categories")
    public Result<List<MedicineCategory>> categories() {
        return Result.success(categoryMapper.selectList(
                new LambdaQueryWrapper<MedicineCategory>()
                        .eq(MedicineCategory::getStatus, 1)
                        .orderByAsc(MedicineCategory::getSortOrder)));
    }

    @GetMapping("/announcements")
    public Result<List<AnnouncementInfo>> announcements() {
        return Result.success(announcementMapper.selectList(
                new LambdaQueryWrapper<AnnouncementInfo>()
                        .eq(AnnouncementInfo::getStatus, 1)
                        .orderByDesc(AnnouncementInfo::getCreatedTime)
                        .last("LIMIT 10")));
    }

    @GetMapping("/medicines")
    public Result<IPage<MedicineVO>> medicines(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return Result.success(medicineService.page(current, size, keyword, categoryId, null, 1));
    }

    @GetMapping("/medicines/{id}")
    public Result<MedicineVO> medicineDetail(@PathVariable Long id) {
        return Result.success(medicineService.getDetail(id));
    }

    @GetMapping("/medicines/hot")
    public Result<List<MedicineVO>> hotMedicines() {
        return Result.success(medicineService.getHotList());
    }
}
