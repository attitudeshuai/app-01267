package com.medicine.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medicine.sales.entity.AdminInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper extends BaseMapper<AdminInfo> {
}
