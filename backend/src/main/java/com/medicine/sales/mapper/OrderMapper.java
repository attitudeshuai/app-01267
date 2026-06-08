package com.medicine.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medicine.sales.entity.OrderInfo;
import com.medicine.sales.vo.DashboardVO;
import com.medicine.sales.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<OrderInfo> {

    @Select("<script>" +
            "SELECT o.*, p.nickname as purchaser_name, mi.company_name as merchant_name " +
            "FROM order_info o " +
            "LEFT JOIN purchaser_info p ON o.purchaser_id = p.id " +
            "LEFT JOIN merchant_info mi ON o.merchant_id = mi.id " +
            "WHERE 1=1 " +
            "<if test='purchaserId != null'>AND o.purchaser_id = #{purchaserId}</if> " +
            "<if test='merchantId != null'>AND o.merchant_id = #{merchantId}</if> " +
            "<if test='orderStatus != null'>AND o.order_status = #{orderStatus}</if> " +
            "<if test='orderNo != null and orderNo != \"\"'>AND o.order_no LIKE CONCAT('%',#{orderNo},'%')</if> " +
            "ORDER BY o.created_time DESC" +
            "</script>")
    IPage<OrderVO> selectOrderPage(Page<?> page,
                                   @Param("purchaserId") Long purchaserId,
                                   @Param("merchantId") Long merchantId,
                                   @Param("orderStatus") Integer orderStatus,
                                   @Param("orderNo") String orderNo);

    @Select("<script>" +
            "SELECT IFNULL(SUM(total_amount), 0) FROM order_info WHERE order_status >= 1 " +
            "<if test='merchantId != null'>AND merchant_id = #{merchantId}</if>" +
            "</script>")
    BigDecimal selectTotalRevenue(@Param("merchantId") Long merchantId);

    @Select("SELECT DATE(created_time) as date, COUNT(*) as count, IFNULL(SUM(total_amount),0) as amount " +
            "FROM order_info WHERE created_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(created_time) ORDER BY date")
    List<DashboardVO.OrderTrend> selectOrderTrends();

    /** 获取采购商历史采购的药材ID列表（与 README Prompt 中 getPurchaseMedicineIds 一致） */
    @Select("SELECT m.id FROM medicine_info m " +
            "INNER JOIN order_detail od ON m.id = od.medicine_id " +
            "INNER JOIN order_info o ON od.order_id = o.id " +
            "WHERE o.purchaser_id = #{purchaserId} " +
            "GROUP BY m.id")
    List<Long> getPurchaseMedicineIds(@Param("purchaserId") Long purchaserId);

    @Select("<script>" +
            "SELECT DISTINCT o.purchaser_id FROM order_info o " +
            "INNER JOIN order_detail od ON o.id = od.order_id " +
            "WHERE od.medicine_id IN " +
            "<foreach collection='medicineIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND o.purchaser_id != #{purchaserId}" +
            "</script>")
    List<Long> getSimilarPurchasers(@Param("medicineIds") List<Long> medicineIds,
                                    @Param("purchaserId") Long purchaserId);

    /** 筛选高相似度用户，获取其采购的药材ID（与 README Prompt 中 getHighSimilarityPurchaseMedicines 一致） */
    @Select("<script>" +
            "SELECT DISTINCT od.medicine_id FROM order_detail od " +
            "INNER JOIN order_info o ON od.order_id = o.id " +
            "WHERE o.purchaser_id IN " +
            "<foreach collection='purchaserIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> getHighSimilarityPurchaseMedicines(@Param("purchaserIds") List<Long> purchaserIds);
}
