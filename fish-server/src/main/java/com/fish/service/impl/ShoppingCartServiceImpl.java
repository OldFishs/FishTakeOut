package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fish.context.BaseContext;
import com.fish.req.ShoppingCart;
import com.fish.entity.DishDO;
import com.fish.entity.SetmealDO;
import com.fish.entity.ShoppingCartDO;
import com.fish.mapper.DishMapper;
import com.fish.mapper.SetmealMapper;
import com.fish.mapper.ShoppingCartMapper;
import com.fish.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public void addShoppingCart(ShoppingCart shoppingCartDTO) {
        ShoppingCartDO shoppingCart = new ShoppingCartDO();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCartDO> list = shoppingCartMapper.selectList(buildCartQueryWrapper(shoppingCart));

        if (list != null && !list.isEmpty()) {
            ShoppingCartDO cart = list.get(0);
            shoppingCartMapper.update(null, Wrappers.lambdaUpdate(ShoppingCartDO.class)
                    .eq(ShoppingCartDO::getId, cart.getId())
                    .set(ShoppingCartDO::getNumber, cart.getNumber() + 1));
        } else {
            shoppingCart.setNumber(1);
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                DishDO dish = dishMapper.selectById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setDishFlavor(shoppingCartDTO.getDishFlavor());
                shoppingCart.setCreateTime(LocalDateTime.now());
            } else {
                SetmealDO setmeal = setmealMapper.selectById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    @Override
    public List<ShoppingCartDO> showShoppingCart() {
        return shoppingCartMapper.selectList(Wrappers.lambdaQuery(ShoppingCartDO.class)
                .eq(ShoppingCartDO::getUserId, BaseContext.getCurrentId()));
    }

    @Override
    public void cleanShoppingCart() {
        shoppingCartMapper.delete(Wrappers.lambdaQuery(ShoppingCartDO.class)
                .eq(ShoppingCartDO::getUserId, BaseContext.getCurrentId()));
    }

    @Override
    public void subShoppingCart(ShoppingCart shoppingCartDTO) {
        ShoppingCartDO shop = new ShoppingCartDO();
        BeanUtils.copyProperties(shoppingCartDTO, shop);
        List<ShoppingCartDO> list = shoppingCartMapper.selectList(buildCartQueryWrapper(shop));
        if (list != null && !list.isEmpty()) {
            ShoppingCartDO cart = list.get(0);
            if (cart.getNumber() == 1) {
                shoppingCartMapper.deleteById(cart.getId());
            } else {
                shoppingCartMapper.update(null, Wrappers.lambdaUpdate(ShoppingCartDO.class)
                        .eq(ShoppingCartDO::getId, cart.getId())
                        .set(ShoppingCartDO::getNumber, cart.getNumber() - 1));
            }
        }
    }

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ShoppingCartDO> buildCartQueryWrapper(ShoppingCartDO shoppingCart) {
        return Wrappers.lambdaQuery(ShoppingCartDO.class)
                .eq(shoppingCart.getUserId() != null, ShoppingCartDO::getUserId, shoppingCart.getUserId())
                .eq(shoppingCart.getDishId() != null, ShoppingCartDO::getDishId, shoppingCart.getDishId())
                .eq(shoppingCart.getSetmealId() != null, ShoppingCartDO::getSetmealId, shoppingCart.getSetmealId())
                .eq(shoppingCart.getDishFlavor() != null, ShoppingCartDO::getDishFlavor, shoppingCart.getDishFlavor());
    }
}
