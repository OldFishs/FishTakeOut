package com.fish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.req.DishPageQuery;
import com.fish.entity.DishDO;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DishMapper extends BaseMapper<DishDO> {

    default Page<DishDO> pageQuery(Page<DishDO> page, DishPageQuery dto) {
        return selectPage(page, Wrappers.lambdaQuery(DishDO.class)
                .like(StringUtils.isNotBlank(dto.getName()), DishDO::getName, dto.getName())
                .eq(dto.getCategoryId() != null, DishDO::getCategoryId, dto.getCategoryId())
                .eq(dto.getStatus() != null, DishDO::getStatus, dto.getStatus())
                .orderByDesc(DishDO::getCreateTime));
    }
}
