package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.constant.MessageConstant;
import com.fish.constant.StatusConstant;
import com.fish.dto.DishDTO;
import com.fish.dto.DishPageQueryDTO;
import com.fish.entity.Category;
import com.fish.entity.Dish;
import com.fish.entity.DishFlavor;
import com.fish.entity.SetmealDish;
import com.fish.exception.DeletionNotAllowedException;
import com.fish.mapper.CategoryMapper;
import com.fish.mapper.DishFlavorMapper;
import com.fish.mapper.DishMapper;
import com.fish.mapper.SetmealDishMapper;
import com.fish.result.PageResult;
import com.fish.service.DishService;
import com.fish.vo.DishVO;
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
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);

        Long dishId = dish.getId();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> flavor.setDishId(dishId));
            flavors.forEach(dishFlavorMapper::insert);
        }
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        Page<Dish> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        page = dishMapper.pageQuery(page, dishPageQueryDTO);
        List<DishVO> dishVOList = buildDishVOList(page.getRecords());
        return new PageResult(page.getTotal(), dishVOList);
    }

    @Override
    @Transactional
    public void deleteBatch(Long[] ids) {
        for (Long id : ids) {
            Dish dish = dishMapper.selectById(id);
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(
                Wrappers.lambdaQuery(SetmealDish.class).in(SetmealDish::getDishId, Arrays.asList(ids)));
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        for (Long id : ids) {
            dishMapper.deleteById(id);
            dishFlavorMapper.delete(Wrappers.lambdaQuery(DishFlavor.class).eq(DishFlavor::getDishId, id));
        }
    }

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.selectById(id);
        DishVO dishVO = new DishVO();
        if (dish != null) {
            BeanUtils.copyProperties(dish, dishVO);
            List<DishFlavor> flavors = dishFlavorMapper.selectList(
                    Wrappers.lambdaQuery(DishFlavor.class).eq(DishFlavor::getDishId, id));
            dishVO.setFlavors(flavors);
        }
        return dishVO;
    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.updateById(dish);

        dishFlavorMapper.delete(Wrappers.lambdaQuery(DishFlavor.class).eq(DishFlavor::getDishId, dish.getId()));

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> flavor.setDishId(dish.getId()));
            flavors.forEach(dishFlavorMapper::insert);
        }
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        dishMapper.update(null, Wrappers.lambdaUpdate(Dish.class)
                .eq(Dish::getId, id)
                .set(Dish::getStatus, status));
    }

    @Override
    public List<Dish> list(Long categoryId) {
        return dishMapper.selectList(Wrappers.lambdaQuery(Dish.class)
                .eq(Dish::getCategoryId, categoryId));
    }

    @Override
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.selectList(Wrappers.lambdaQuery(Dish.class)
                .like(StringUtils.isNotBlank(dish.getName()), Dish::getName, dish.getName())
                .eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId())
                .eq(dish.getStatus() != null, Dish::getStatus, dish.getStatus())
                .orderByDesc(Dish::getCreateTime));

        List<DishVO> dishVOList = new ArrayList<>();
        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);
            List<DishFlavor> flavors = dishFlavorMapper.selectList(
                    Wrappers.lambdaQuery(DishFlavor.class).eq(DishFlavor::getDishId, d.getId()));
            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }
        return dishVOList;
    }

    private List<DishVO> buildDishVOList(List<Dish> dishes) {
        if (dishes == null || dishes.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> categoryIds = dishes.stream().map(Dish::getCategoryId).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<DishVO> dishVOList = new ArrayList<>();
        for (Dish dish : dishes) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            dishVO.setCategoryName(categoryNameMap.get(dish.getCategoryId()));
            dishVOList.add(dishVO);
        }
        return dishVOList;
    }
}
