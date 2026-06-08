package com.medicine.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medicine.sales.entity.OrderDetail;
import com.medicine.sales.vo.DashboardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

    @Select("SELECT od.medicine_id, od.medicine_name, SUM(od.quantity) as sales_count " +
            "FROM order_detail od " +
            "INNER JOIN order_info o ON od.order_id = o.id " +
            "WHERE o.order_status >= 1 " +
            "GROUP BY od.medicine_id, od.medicine_name " +
            "ORDER BY sales_count DESC LIMIT 10")
    List<DashboardVO.HotMedicine> selectHotMedicines();
}
