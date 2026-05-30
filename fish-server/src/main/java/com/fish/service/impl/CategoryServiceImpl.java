package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.constant.MessageConstant;
import com.fish.constant.StatusConstant;
import com.fish.context.BaseContext;
import com.fish.dto.CategoryDTO;
import com.fish.dto.CategoryPageQueryDTO;
import com.fish.entity.Category;
import com.fish.entity.Dish;
import com.fish.entity.Setmeal;
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
    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        category.setStatus(StatusConstant.DISABLE);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setCreateUser(BaseContext.getCurrentId());
        category.setUpdateUser(BaseContext.getCurrentId());
        categoryMapper.insert(category);
    }

    @Override
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        Page<Category> page = new Page<>(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());
        page = categoryMapper.pageQuery(page, categoryPageQueryDTO);
        return new PageResult(page.getTotal(), page.getRecords());
    }

    @Override
    public void deleteById(Long id) {
        Long dishCount = dishMapper.selectCount(Wrappers.lambdaQuery(Dish.class).eq(Dish::getCategoryId, id));
        if (dishCount > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        Long setmealCount = setmealMapper.selectCount(Wrappers.lambdaQuery(Setmeal.class).eq(Setmeal::getCategoryId, id));
        if (setmealCount > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        categoryMapper.deleteById(id);
    }

    @Override
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        categoryMapper.update(null, Wrappers.lambdaUpdate(Category.class)
                .eq(Category::getId, category.getId())
                .set(category.getType() != null, Category::getType, category.getType())
                .set(StringUtils.isNotBlank(category.getName()), Category::getName, category.getName())
                .set(category.getSort() != null, Category::getSort, category.getSort())
                .set(category.getStatus() != null, Category::getStatus, category.getStatus())
                .set(Category::getUpdateTime, LocalDateTime.now())
                .set(Category::getUpdateUser, BaseContext.getCurrentId()));
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        categoryMapper.update(null, Wrappers.lambdaUpdate(Category.class)
                .eq(Category::getId, id)
                .set(Category::getStatus, status)
                .set(Category::getUpdateTime, LocalDateTime.now())
                .set(Category::getUpdateUser, BaseContext.getCurrentId()));
    }

    @Override
    public List<Category> list(Integer type) {
        return categoryMapper.selectList(Wrappers.lambdaQuery(Category.class)
                .eq(Category::getStatus, StatusConstant.ENABLE)
                .eq(type != null, Category::getType, type)
                .orderByAsc(Category::getSort)
                .orderByDesc(Category::getCreateTime));
    }
}
