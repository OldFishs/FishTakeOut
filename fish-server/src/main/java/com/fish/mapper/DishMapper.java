package com.fish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.dto.DishPageQueryDTO;
import com.fish.entity.Dish;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    default Page<Dish> pageQuery(Page<Dish> page, DishPageQueryDTO dto) {
        return selectPage(page, Wrappers.lambdaQuery(Dish.class)
                .like(StringUtils.isNotBlank(dto.getName()), Dish::getName, dto.getName())
                .eq(dto.getCategoryId() != null, Dish::getCategoryId, dto.getCategoryId())
                .eq(dto.getStatus() != null, Dish::getStatus, dto.getStatus())
                .orderByDesc(Dish::getCreateTime));
    }
}
