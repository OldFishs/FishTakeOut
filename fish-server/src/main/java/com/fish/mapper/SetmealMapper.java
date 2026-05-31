package com.fish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.req.SetmealPageQuery;
import com.fish.entity.SetmealDO;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SetmealMapper extends BaseMapper<SetmealDO> {

    default Page<SetmealDO> pageQuery(Page<SetmealDO> page, SetmealPageQuery dto) {
        return selectPage(page, Wrappers.lambdaQuery(SetmealDO.class)
                .like(StringUtils.isNotBlank(dto.getName()), SetmealDO::getName, dto.getName())
                .eq(dto.getStatus() != null, SetmealDO::getStatus, dto.getStatus())
                .eq(dto.getCategoryId() != null, SetmealDO::getCategoryId, dto.getCategoryId())
                .orderByDesc(SetmealDO::getCreateTime));
    }
}
