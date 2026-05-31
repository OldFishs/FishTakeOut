package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.req.Setmeal;
import com.fish.req.SetmealPageQuery;
import com.fish.entity.CategoryDO;
import com.fish.entity.DishDO;
import com.fish.entity.SetmealDO;
import com.fish.entity.SetmealDishDO;
import com.fish.mapper.CategoryMapper;
import com.fish.mapper.DishMapper;
import com.fish.mapper.SetmealDishMapper;
import com.fish.mapper.SetmealMapper;
import com.fish.result.PageResult;
import com.fish.service.SetmealService;
import com.fish.resp.DishItemVO;
import com.fish.resp.SetmealVO;
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
    public void saveWithDish(Setmeal setmealDTO) {
        SetmealDO setmeal = new SetmealDO();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);

        List<SetmealDishDO> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
        setmealDishes.forEach(setmealDishMapper::insert);
    }

    @Override
    public PageResult pageQuery(SetmealPageQuery setmealPageQueryDTO) {
        Page<SetmealDO> page = new Page<>(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
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
    public void update(Setmeal setmealDTO) {
        SetmealDO setmeal = new SetmealDO();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.updateById(setmeal);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        setmealMapper.update(null, Wrappers.lambdaUpdate(SetmealDO.class)
                .eq(SetmealDO::getId, id)
                .set(SetmealDO::getStatus, status));
    }

    @Override
    public List<SetmealDO> list(SetmealDO setmeal) {
        return setmealMapper.selectList(Wrappers.lambdaQuery(SetmealDO.class)
                .like(StringUtils.isNotBlank(setmeal.getName()), SetmealDO::getName, setmeal.getName())
                .eq(setmeal.getCategoryId() != null, SetmealDO::getCategoryId, setmeal.getCategoryId())
                .eq(setmeal.getStatus() != null, SetmealDO::getStatus, setmeal.getStatus()));
    }

    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        return getDishItemBySetmealId(id);
    }

    @Override
    public SetmealVO getById(Long id) {
        SetmealDO setmeal = setmealMapper.selectById(id);
        if (setmeal == null) {
            return null;
        }
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);

        CategoryDO category = categoryMapper.selectById(setmeal.getCategoryId());
        if (category != null) {
            setmealVO.setCategoryName(category.getName());
        }

        List<SetmealDishDO> setmealDishes = setmealDishMapper.selectList(
                Wrappers.lambdaQuery(SetmealDishDO.class).eq(SetmealDishDO::getSetmealId, id));
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    private List<DishItemVO> getDishItemBySetmealId(Long setmealId) {
        List<SetmealDishDO> setmealDishes = setmealDishMapper.selectList(
                Wrappers.lambdaQuery(SetmealDishDO.class).eq(SetmealDishDO::getSetmealId, setmealId));

        List<DishItemVO> dishItemVOList = new ArrayList<>();
        for (SetmealDishDO setmealDish : setmealDishes) {
            DishItemVO dishItemVO = DishItemVO.builder()
                    .name(setmealDish.getName())
                    .copies(setmealDish.getCopies())
                    .build();
            DishDO dish = dishMapper.selectById(setmealDish.getDishId());
            if (dish != null) {
                dishItemVO.setImage(dish.getImage());
                dishItemVO.setDescription(dish.getDescription());
            }
            dishItemVOList.add(dishItemVO);
        }
        return dishItemVOList;
    }

    private List<SetmealVO> buildSetmealVOList(List<SetmealDO> setmeals) {
        if (setmeals == null || setmeals.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> categoryIds = setmeals.stream().map(SetmealDO::getCategoryId).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(CategoryDO::getId, CategoryDO::getName));

        List<SetmealVO> setmealVOList = new ArrayList<>();
        for (SetmealDO setmeal : setmeals) {
            SetmealVO setmealVO = new SetmealVO();
            BeanUtils.copyProperties(setmeal, setmealVO);
            setmealVO.setCategoryName(categoryNameMap.get(setmeal.getCategoryId()));
            setmealVOList.add(setmealVO);
        }
        return setmealVOList;
    }
}
