package com.fish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.dto.OrdersPageQueryDTO;
import com.fish.entity.Orders;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    default Page<Orders> pageQuery(Page<Orders> page, OrdersPageQueryDTO dto) {
        return selectPage(page, Wrappers.lambdaQuery(Orders.class)
                .like(StringUtils.isNotBlank(dto.getNumber()), Orders::getNumber, dto.getNumber())
                .like(StringUtils.isNotBlank(dto.getPhone()), Orders::getPhone, dto.getPhone())
                .eq(dto.getUserId() != null, Orders::getUserId, dto.getUserId())
                .eq(dto.getStatus() != null, Orders::getStatus, dto.getStatus())
                .ge(dto.getBeginTime() != null, Orders::getOrderTime, dto.getBeginTime())
                .le(dto.getEndTime() != null, Orders::getOrderTime, dto.getEndTime())
                .orderByDesc(Orders::getOrderTime));
    }
}
