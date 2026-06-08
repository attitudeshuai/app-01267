package com.medicine.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medicine.sales.entity.InventoryInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InventoryMapper extends BaseMapper<InventoryInfo> {

    @Select("SELECT * FROM inventory_info WHERE stock_quantity <= warning_threshold")
    List<InventoryInfo> selectLowStockInventories();
}
