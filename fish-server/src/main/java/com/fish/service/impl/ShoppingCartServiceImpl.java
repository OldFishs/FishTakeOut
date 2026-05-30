package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fish.context.BaseContext;
import com.fish.dto.ShoppingCartDTO;
import com.fish.entity.Dish;
import com.fish.entity.Setmeal;
import com.fish.entity.ShoppingCart;
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
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.selectList(buildCartQueryWrapper(shoppingCart));

        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            shoppingCartMapper.update(null, Wrappers.lambdaUpdate(ShoppingCart.class)
                    .eq(ShoppingCart::getId, cart.getId())
                    .set(ShoppingCart::getNumber, cart.getNumber() + 1));
        } else {
            shoppingCart.setNumber(1);
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                Dish dish = dishMapper.selectById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setDishFlavor(shoppingCartDTO.getDishFlavor());
                shoppingCart.setCreateTime(LocalDateTime.now());
            } else {
                Setmeal setmeal = setmealMapper.selectById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        return shoppingCartMapper.selectList(Wrappers.lambdaQuery(ShoppingCart.class)
                .eq(ShoppingCart::getUserId, BaseContext.getCurrentId()));
    }

    @Override
    public void cleanShoppingCart() {
        shoppingCartMapper.delete(Wrappers.lambdaQuery(ShoppingCart.class)
                .eq(ShoppingCart::getUserId, BaseContext.getCurrentId()));
    }

    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shop = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shop);
        List<ShoppingCart> list = shoppingCartMapper.selectList(buildCartQueryWrapper(shop));
        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            if (cart.getNumber() == 1) {
                shoppingCartMapper.deleteById(cart.getId());
            } else {
                shoppingCartMapper.update(null, Wrappers.lambdaUpdate(ShoppingCart.class)
                        .eq(ShoppingCart::getId, cart.getId())
                        .set(ShoppingCart::getNumber, cart.getNumber() - 1));
            }
        }
    }

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ShoppingCart> buildCartQueryWrapper(ShoppingCart shoppingCart) {
        return Wrappers.lambdaQuery(ShoppingCart.class)
                .eq(shoppingCart.getUserId() != null, ShoppingCart::getUserId, shoppingCart.getUserId())
                .eq(shoppingCart.getDishId() != null, ShoppingCart::getDishId, shoppingCart.getDishId())
                .eq(shoppingCart.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCart.getSetmealId())
                .eq(shoppingCart.getDishFlavor() != null, ShoppingCart::getDishFlavor, shoppingCart.getDishFlavor());
    }
}
