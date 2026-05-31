package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.constant.MessageConstant;
import com.fish.constant.StatusConstant;
import com.fish.req.Dish;
import com.fish.req.DishPageQuery;
import com.fish.entity.CategoryDO;
import com.fish.entity.DishDO;
import com.fish.entity.DishFlavorDO;
import com.fish.entity.SetmealDishDO;
import com.fish.exception.DeletionNotAllowedException;
import com.fish.mapper.CategoryMapper;
import com.fish.mapper.DishFlavorMapper;
import com.fish.mapper.DishMapper;
import com.fish.mapper.SetmealDishMapper;
import com.fish.result.PageResult;
import com.fish.service.DishService;
import com.fish.resp.DishVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    @Transactional
    public void saveWithFlavor(Dish dishDTO) {
        DishDO dish = new DishDO();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);

        Long dishId = dish.getId();
        List<DishFlavorDO> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> flavor.setDishId(dishId));
            flavors.forEach(dishFlavorMapper::insert);
        }
    }

    @Override
    public PageResult pageQuery(DishPageQuery dishPageQueryDTO) {
        Page<DishDO> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        page = dishMapper.pageQuery(page, dishPageQueryDTO);
        List<DishVO> dishVOList = buildDishVOList(page.getRecords());
        return new PageResult(page.getTotal(), dishVOList);
    }

    @Override
    @Transactional
    public void deleteBatch(Long[] ids) {
        for (Long id : ids) {
            DishDO dish = dishMapper.selectById(id);
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        List<SetmealDishDO> setmealDishes = setmealDishMapper.selectList(
                Wrappers.lambdaQuery(SetmealDishDO.class).in(SetmealDishDO::getDishId, Arrays.asList(ids)));
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        for (Long id : ids) {
            dishMapper.deleteById(id);
            dishFlavorMapper.delete(Wrappers.lambdaQuery(DishFlavorDO.class).eq(DishFlavorDO::getDishId, id));
        }
    }

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        DishDO dish = dishMapper.selectById(id);
        DishVO dishVO = new DishVO();
        if (dish != null) {
            BeanUtils.copyProperties(dish, dishVO);
            List<DishFlavorDO> flavors = dishFlavorMapper.selectList(
                    Wrappers.lambdaQuery(DishFlavorDO.class).eq(DishFlavorDO::getDishId, id));
            dishVO.setFlavors(flavors);
        }
        return dishVO;
    }

    @Override
    @Transactional
    public void updateWithFlavor(Dish dishDTO) {
        DishDO dish = new DishDO();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.updateById(dish);

        dishFlavorMapper.delete(Wrappers.lambdaQuery(DishFlavorDO.class).eq(DishFlavorDO::getDishId, dish.getId()));

        List<DishFlavorDO> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> flavor.setDishId(dish.getId()));
            flavors.forEach(dishFlavorMapper::insert);
        }
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        dishMapper.update(null, Wrappers.lambdaUpdate(DishDO.class)
                .eq(DishDO::getId, id)
                .set(DishDO::getStatus, status));
    }

    @Override
    public List<DishDO> list(Long categoryId) {
        return dishMapper.selectList(Wrappers.lambdaQuery(DishDO.class)
                .eq(DishDO::getCategoryId, categoryId));
    }

    @Override
    public List<DishVO> listWithFlavor(DishDO dish) {
        List<DishDO> dishList = dishMapper.selectList(Wrappers.lambdaQuery(DishDO.class)
                .like(StringUtils.isNotBlank(dish.getName()), DishDO::getName, dish.getName())
                .eq(dish.getCategoryId() != null, DishDO::getCategoryId, dish.getCategoryId())
                .eq(dish.getStatus() != null, DishDO::getStatus, dish.getStatus())
                .orderByDesc(DishDO::getCreateTime));

        List<DishVO> dishVOList = new ArrayList<>();
        for (DishDO d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);
            List<DishFlavorDO> flavors = dishFlavorMapper.selectList(
                    Wrappers.lambdaQuery(DishFlavorDO.class).eq(DishFlavorDO::getDishId, d.getId()));
            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }
        return dishVOList;
    }

    private List<DishVO> buildDishVOList(List<DishDO> dishes) {
        if (dishes == null || dishes.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> categoryIds = dishes.stream().map(DishDO::getCategoryId).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(CategoryDO::getId, CategoryDO::getName));

        List<DishVO> dishVOList = new ArrayList<>();
        for (DishDO dish : dishes) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            dishVO.setCategoryName(categoryNameMap.get(dish.getCategoryId()));
            dishVOList.add(dishVO);
        }
        return dishVOList;
    }
}
