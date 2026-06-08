package com.medicine.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.sales.entity.MedicineInfo;
import com.medicine.sales.vo.MedicineVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MedicineMapper extends BaseMapper<MedicineInfo> {

    @Select("<script>" +
            "SELECT m.*, mc.name as category_name, mi.company_name as merchant_name, " +
            "IFNULL(inv.stock_quantity, 0) as stock_quantity, IFNULL(inv.warning_threshold, 10) as warning_threshold " +
            "FROM medicine_info m " +
            "LEFT JOIN medicine_category mc ON m.category_id = mc.id " +
            "LEFT JOIN merchant_info mi ON m.merchant_id = mi.id " +
            "LEFT JOIN inventory_info inv ON m.id = inv.medicine_id AND m.merchant_id = inv.merchant_id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>AND m.name LIKE CONCAT('%',#{keyword},'%')</if> " +
            "<if test='categoryId != null'>AND m.category_id = #{categoryId}</if> " +
            "<if test='merchantId != null'>AND m.merchant_id = #{merchantId}</if> " +
            "<if test='status != null'>AND m.status = #{status}</if> " +
            "ORDER BY m.created_time DESC" +
            "</script>")
    IPage<MedicineVO> selectMedicinePage(Page<?> page,
                                         @Param("keyword") String keyword,
                                         @Param("categoryId") Long categoryId,
                                         @Param("merchantId") Long merchantId,
                                         @Param("status") Integer status);

    /** 过滤当前用户已采购的药材，按销量排序（与 README Prompt 中 getRecommendMedicines 一致） */
    @Select("<script>" +
            "SELECT m.*, mc.name as category_name, mi.company_name as merchant_name, " +
            "IFNULL(inv.stock_quantity, 0) as stock_quantity, IFNULL(inv.warning_threshold, 10) as warning_threshold " +
            "FROM medicine_info m " +
            "LEFT JOIN medicine_category mc ON m.category_id = mc.id " +
            "LEFT JOIN merchant_info mi ON m.merchant_id = mi.id " +
            "LEFT JOIN inventory_info inv ON m.id = inv.medicine_id AND m.merchant_id = inv.merchant_id " +
            "WHERE m.id IN " +
            "<foreach collection='medicineIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND m.id NOT IN (SELECT od.medicine_id FROM order_detail od " +
            "INNER JOIN order_info o ON od.order_id = o.id WHERE o.purchaser_id = #{purchaserId}) " +
            "AND m.status = 1 " +
            "ORDER BY m.sales_count DESC LIMIT #{limit}" +
            "</script>")
    List<MedicineVO> getRecommendMedicines(@Param("medicineIds") List<Long> medicineIds,
                                          @Param("purchaserId") Long purchaserId,
                                          @Param("limit") int limit);

}
