package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.dto.SetmealDTO;
import com.fish.dto.SetmealPageQueryDTO;
import com.fish.entity.Category;
import com.fish.entity.Dish;
import com.fish.entity.Setmeal;
import com.fish.entity.SetmealDish;
import com.fish.mapper.CategoryMapper;
import com.fish.mapper.DishMapper;
import com.fish.mapper.SetmealDishMapper;
import com.fish.mapper.SetmealMapper;
import com.fish.result.PageResult;
import com.fish.service.SetmealService;
import com.fish.vo.DishItemVO;
import com.fish.vo.SetmealVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
        setmealDishes.forEach(setmealDishMapper::insert);
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        Page<Setmeal> page = new Page<>(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        page = setmealMapper.pageQuery(page, setmealPageQueryDTO);
        List<SetmealVO> setmealVOList = buildSetmealVOList(page.getRecords());
        return new PageResult(page.getTotal(), setmealVOList);
    }

    @Override
    public void deleteBatch(List<Long> ids) {
    }

    @Override
    public SetmealVO getByIdWithDish(Long id) {
        return getById(id);
    }

    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.updateById(setmeal);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        setmealMapper.update(null, Wrappers.lambdaUpdate(Setmeal.class)
                .eq(Setmeal::getId, id)
                .set(Setmeal::getStatus, status));
    }

    @Override
    public List<Setmeal> list(Setmeal setmeal) {
        return setmealMapper.selectList(Wrappers.lambdaQuery(Setmeal.class)
                .like(StringUtils.isNotBlank(setmeal.getName()), Setmeal::getName, setmeal.getName())
                .eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId())
                .eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus()));
    }

    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        return getDishItemBySetmealId(id);
    }

    @Override
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.selectById(id);
        if (setmeal == null) {
            return null;
        }
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);

        Category category = categoryMapper.selectById(setmeal.getCategoryId());
        if (category != null) {
            setmealVO.setCategoryName(category.getName());
        }

        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(
                Wrappers.lambdaQuery(SetmealDish.class).eq(SetmealDish::getSetmealId, id));
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    private List<DishItemVO> getDishItemBySetmealId(Long setmealId) {
        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(
                Wrappers.lambdaQuery(SetmealDish.class).eq(SetmealDish::getSetmealId, setmealId));

        List<DishItemVO> dishItemVOList = new ArrayList<>();
        for (SetmealDish setmealDish : setmealDishes) {
            DishItemVO dishItemVO = DishItemVO.builder()
                    .name(setmealDish.getName())
                    .copies(setmealDish.getCopies())
                    .build();
            Dish dish = dishMapper.selectById(setmealDish.getDishId());
            if (dish != null) {
                dishItemVO.setImage(dish.getImage());
                dishItemVO.setDescription(dish.getDescription());
            }
            dishItemVOList.add(dishItemVO);
        }
        return dishItemVOList;
    }

    private List<SetmealVO> buildSetmealVOList(List<Setmeal> setmeals) {
        if (setmeals == null || setmeals.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> categoryIds = setmeals.stream().map(Setmeal::getCategoryId).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<SetmealVO> setmealVOList = new ArrayList<>();
        for (Setmeal setmeal : setmeals) {
            SetmealVO setmealVO = new SetmealVO();
            BeanUtils.copyProperties(setmeal, setmealVO);
            setmealVO.setCategoryName(categoryNameMap.get(setmeal.getCategoryId()));
            setmealVOList.add(setmealVO);
        }
        return setmealVOList;
    }
}
