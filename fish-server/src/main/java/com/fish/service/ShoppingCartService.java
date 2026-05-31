package com.fish.service;

import com.fish.req.ShoppingCart;
import com.fish.entity.ShoppingCartDO;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCart shoppingCartDTO);

    /**
     * 查看购物车
     * @return
     */
    List<ShoppingCartDO> showShoppingCart();

    /**
     * 清空购物车
     */
    void cleanShoppingCart();

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    void subShoppingCart(ShoppingCart shoppingCartDTO);
}
