package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.constant.MessageConstant;
import com.fish.constant.StatusConstant;
import com.fish.context.BaseContext;
import com.fish.req.Category;
import com.fish.req.CategoryPageQuery;
import com.fish.entity.CategoryDO;
import com.fish.entity.DishDO;
import com.fish.entity.SetmealDO;
import com.fish.exception.DeletionNotAllowedException;
import com.fish.mapper.CategoryMapper;
import com.fish.mapper.DishMapper;
import com.fish.mapper.SetmealMapper;
import com.fish.result.PageResult;
import com.fish.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public void save(Category categoryDTO) {
        CategoryDO category = new CategoryDO();
        BeanUtils.copyProperties(categoryDTO, category);
        category.setStatus(StatusConstant.DISABLE);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setCreateUser(BaseContext.getCurrentId());
        category.setUpdateUser(BaseContext.getCurrentId());
        categoryMapper.insert(category);
    }

    @Override
    public PageResult pageQuery(CategoryPageQuery categoryPageQueryDTO) {
        Page<CategoryDO> page = new Page<>(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());
        page = categoryMapper.pageQuery(page, categoryPageQueryDTO);
        return new PageResult(page.getTotal(), page.getRecords());
    }

    @Override
    public void deleteById(Long id) {
        Long dishCount = dishMapper.selectCount(Wrappers.lambdaQuery(DishDO.class).eq(DishDO::getCategoryId, id));
        if (dishCount > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        Long setmealCount = setmealMapper.selectCount(Wrappers.lambdaQuery(SetmealDO.class).eq(SetmealDO::getCategoryId, id));
        if (setmealCount > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        categoryMapper.deleteById(id);
    }

    @Override
    public void update(Category categoryDTO) {
        CategoryDO category = new CategoryDO();
        BeanUtils.copyProperties(categoryDTO, category);
        categoryMapper.update(null, Wrappers.lambdaUpdate(CategoryDO.class)
                .eq(CategoryDO::getId, category.getId())
                .set(category.getType() != null, CategoryDO::getType, category.getType())
                .set(StringUtils.isNotBlank(category.getName()), CategoryDO::getName, category.getName())
                .set(category.getSort() != null, CategoryDO::getSort, category.getSort())
                .set(category.getStatus() != null, CategoryDO::getStatus, category.getStatus())
                .set(CategoryDO::getUpdateTime, LocalDateTime.now())
                .set(CategoryDO::getUpdateUser, BaseContext.getCurrentId()));
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        categoryMapper.update(null, Wrappers.lambdaUpdate(CategoryDO.class)
                .eq(CategoryDO::getId, id)
                .set(CategoryDO::getStatus, status)
                .set(CategoryDO::getUpdateTime, LocalDateTime.now())
                .set(CategoryDO::getUpdateUser, BaseContext.getCurrentId()));
    }

    @Override
    public List<CategoryDO> list(Integer type) {
        return categoryMapper.selectList(Wrappers.lambdaQuery(CategoryDO.class)
                .eq(CategoryDO::getStatus, StatusConstant.ENABLE)
                .eq(type != null, CategoryDO::getType, type)
                .orderByAsc(CategoryDO::getSort)
                .orderByDesc(CategoryDO::getCreateTime));
    }
}
