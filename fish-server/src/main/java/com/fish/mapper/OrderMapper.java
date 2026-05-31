package com.fish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.req.OrdersPageQuery;
import com.fish.entity.OrdersDO;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrdersDO> {

    default Page<OrdersDO> pageQuery(Page<OrdersDO> page, OrdersPageQuery dto) {
        return selectPage(page, Wrappers.lambdaQuery(OrdersDO.class)
                .like(StringUtils.isNotBlank(dto.getNumber()), OrdersDO::getNumber, dto.getNumber())
                .like(StringUtils.isNotBlank(dto.getPhone()), OrdersDO::getPhone, dto.getPhone())
                .eq(dto.getUserId() != null, OrdersDO::getUserId, dto.getUserId())
                .eq(dto.getStatus() != null, OrdersDO::getStatus, dto.getStatus())
                .ge(dto.getBeginTime() != null, OrdersDO::getOrderTime, dto.getBeginTime())
                .le(dto.getEndTime() != null, OrdersDO::getOrderTime, dto.getEndTime())
                .orderByDesc(OrdersDO::getOrderTime));
    }
}
