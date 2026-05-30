package com.fish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.dto.CategoryPageQueryDTO;
import com.fish.entity.Category;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    default Page<Category> pageQuery(Page<Category> page, CategoryPageQueryDTO dto) {
        return selectPage(page, Wrappers.lambdaQuery(Category.class)
                .like(StringUtils.isNotBlank(dto.getName()), Category::getName, dto.getName())
                .eq(dto.getType() != null, Category::getType, dto.getType())
                .orderByAsc(Category::getSort)
                .orderByDesc(Category::getCreateTime));
    }
}
