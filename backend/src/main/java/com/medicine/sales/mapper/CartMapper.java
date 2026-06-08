package com.medicine.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medicine.sales.entity.CartInfo;
import com.medicine.sales.vo.CartVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<CartInfo> {

    @Select("SELECT c.id, c.medicine_id, m.name as medicine_name, m.image as medicine_image, " +
            "m.price, c.quantity, c.merchant_id, mi.company_name as merchant_name, " +
            "IFNULL(inv.stock_quantity, 0) as stock_quantity " +
            "FROM cart_info c " +
            "LEFT JOIN medicine_info m ON c.medicine_id = m.id " +
            "LEFT JOIN merchant_info mi ON c.merchant_id = mi.id " +
            "LEFT JOIN inventory_info inv ON c.medicine_id = inv.medicine_id AND c.merchant_id = inv.merchant_id " +
            "WHERE c.purchaser_id = #{purchaserId} " +
            "ORDER BY c.created_time DESC")
    List<CartVO> selectCartList(@Param("purchaserId") Long purchaserId);
}
