package com.fish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.dto.SetmealPageQueryDTO;
import com.fish.entity.Setmeal;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SetmealMapper extends BaseMapper<Setmeal> {

    default Page<Setmeal> pageQuery(Page<Setmeal> page, SetmealPageQueryDTO dto) {
        return selectPage(page, Wrappers.lambdaQuery(Setmeal.class)
                .like(StringUtils.isNotBlank(dto.getName()), Setmeal::getName, dto.getName())
                .eq(dto.getStatus() != null, Setmeal::getStatus, dto.getStatus())
                .eq(dto.getCategoryId() != null, Setmeal::getCategoryId, dto.getCategoryId())
                .orderByDesc(Setmeal::getCreateTime));
    }
}
