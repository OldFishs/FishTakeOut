package com.fish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.req.CategoryPageQuery;
import com.fish.entity.CategoryDO;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<CategoryDO> {

    default Page<CategoryDO> pageQuery(Page<CategoryDO> page, CategoryPageQuery dto) {
        return selectPage(page, Wrappers.lambdaQuery(CategoryDO.class)
                .like(StringUtils.isNotBlank(dto.getName()), CategoryDO::getName, dto.getName())
                .eq(dto.getType() != null, CategoryDO::getType, dto.getType())
                .orderByAsc(CategoryDO::getSort)
                .orderByDesc(CategoryDO::getCreateTime));
    }
}
