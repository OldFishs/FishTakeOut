package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fish.constant.StatusConstant;
import com.fish.entity.Dish;
import com.fish.entity.Orders;
import com.fish.entity.Setmeal;
import com.fish.entity.User;
import com.fish.mapper.DishMapper;
import com.fish.mapper.OrderMapper;
import com.fish.mapper.SetmealMapper;
import com.fish.mapper.UserMapper;
import com.fish.service.WorkspaceService;
import com.fish.vo.BusinessDataVO;
import com.fish.vo.DishOverViewVO;
import com.fish.vo.OrderOverViewVO;
import com.fish.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        Integer totalOrderCount = Math.toIntExact(countOrders(begin, end, null));

        Integer validOrderCount = Math.toIntExact(countOrders(begin, end, Orders.COMPLETED));
        Double turnover = sumOrderAmount(begin, end, Orders.COMPLETED);
        turnover = turnover == null ? 0.0 : turnover;

        Double unitPrice = 0.0;
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0 && validOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            unitPrice = Double.parseDouble(String.format("%.2f", turnover / validOrderCount));
        }

        Integer newUsers = Math.toIntExact(countUsers(begin, end));

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    @Override
    public OrderOverViewVO getOrderOverView() {
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);

        Integer waitingOrders = Math.toIntExact(countOrders(begin, null, Orders.TO_BE_CONFIRMED));
        Integer deliveredOrders = Math.toIntExact(countOrders(begin, null, Orders.CONFIRMED));
        Integer completedOrders = Math.toIntExact(countOrders(begin, null, Orders.COMPLETED));
        Integer cancelledOrders = Math.toIntExact(countOrders(begin, null, Orders.CANCELLED));
        Integer allOrders = Math.toIntExact(countOrders(begin, null, null));

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    @Override
    public DishOverViewVO getDishOverView() {
        Integer sold = Math.toIntExact(dishMapper.selectCount(
                Wrappers.lambdaQuery(Dish.class).eq(Dish::getStatus, StatusConstant.ENABLE)));
        Integer discontinued = Math.toIntExact(dishMapper.selectCount(
                Wrappers.lambdaQuery(Dish.class).eq(Dish::getStatus, StatusConstant.DISABLE)));

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    @Override
    public SetmealOverViewVO getSetmealOverView() {
        Integer sold = Math.toIntExact(setmealMapper.selectCount(
                Wrappers.lambdaQuery(Setmeal.class).eq(Setmeal::getStatus, StatusConstant.ENABLE)));
        Integer discontinued = Math.toIntExact(setmealMapper.selectCount(
                Wrappers.lambdaQuery(Setmeal.class).eq(Setmeal::getStatus, StatusConstant.DISABLE)));

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    private Long countOrders(LocalDateTime begin, LocalDateTime end, Integer status) {
        return orderMapper.selectCount(Wrappers.lambdaQuery(Orders.class)
                .gt(begin != null, Orders::getOrderTime, begin)
                .lt(end != null, Orders::getOrderTime, end)
                .eq(status != null, Orders::getStatus, status));
    }

    private Double sumOrderAmount(LocalDateTime begin, LocalDateTime end, Integer status) {
        List<Orders> orders = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class)
                .gt(begin != null, Orders::getOrderTime, begin)
                .lt(end != null, Orders::getOrderTime, end)
                .eq(status != null, Orders::getStatus, status));
        return orders.stream()
                .map(Orders::getAmount)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
    }

    private Long countUsers(LocalDateTime begin, LocalDateTime end) {
        return userMapper.selectCount(Wrappers.lambdaQuery(User.class)
                .gt(begin != null, User::getCreateTime, begin)
                .lt(end != null, User::getCreateTime, end));
    }
}
