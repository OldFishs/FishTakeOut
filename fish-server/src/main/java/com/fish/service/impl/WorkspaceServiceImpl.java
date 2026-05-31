package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fish.constant.StatusConstant;
import com.fish.entity.DishDO;
import com.fish.entity.OrdersDO;
import com.fish.entity.SetmealDO;
import com.fish.entity.UserDO;
import com.fish.mapper.DishMapper;
import com.fish.mapper.OrderMapper;
import com.fish.mapper.SetmealMapper;
import com.fish.mapper.UserMapper;
import com.fish.service.WorkspaceService;
import com.fish.resp.BusinessDataVO;
import com.fish.resp.DishOverViewVO;
import com.fish.resp.OrderOverViewVO;
import com.fish.resp.SetmealOverViewVO;
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

        Integer validOrderCount = Math.toIntExact(countOrders(begin, end, OrdersDO.COMPLETED));
        Double turnover = sumOrderAmount(begin, end, OrdersDO.COMPLETED);
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

        Integer waitingOrders = Math.toIntExact(countOrders(begin, null, OrdersDO.TO_BE_CONFIRMED));
        Integer deliveredOrders = Math.toIntExact(countOrders(begin, null, OrdersDO.CONFIRMED));
        Integer completedOrders = Math.toIntExact(countOrders(begin, null, OrdersDO.COMPLETED));
        Integer cancelledOrders = Math.toIntExact(countOrders(begin, null, OrdersDO.CANCELLED));
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
                Wrappers.lambdaQuery(DishDO.class).eq(DishDO::getStatus, StatusConstant.ENABLE)));
        Integer discontinued = Math.toIntExact(dishMapper.selectCount(
                Wrappers.lambdaQuery(DishDO.class).eq(DishDO::getStatus, StatusConstant.DISABLE)));

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    @Override
    public SetmealOverViewVO getSetmealOverView() {
        Integer sold = Math.toIntExact(setmealMapper.selectCount(
                Wrappers.lambdaQuery(SetmealDO.class).eq(SetmealDO::getStatus, StatusConstant.ENABLE)));
        Integer discontinued = Math.toIntExact(setmealMapper.selectCount(
                Wrappers.lambdaQuery(SetmealDO.class).eq(SetmealDO::getStatus, StatusConstant.DISABLE)));

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    private Long countOrders(LocalDateTime begin, LocalDateTime end, Integer status) {
        return orderMapper.selectCount(Wrappers.lambdaQuery(OrdersDO.class)
                .gt(begin != null, OrdersDO::getOrderTime, begin)
                .lt(end != null, OrdersDO::getOrderTime, end)
                .eq(status != null, OrdersDO::getStatus, status));
    }

    private Double sumOrderAmount(LocalDateTime begin, LocalDateTime end, Integer status) {
        List<OrdersDO> orders = orderMapper.selectList(Wrappers.lambdaQuery(OrdersDO.class)
                .gt(begin != null, OrdersDO::getOrderTime, begin)
                .lt(end != null, OrdersDO::getOrderTime, end)
                .eq(status != null, OrdersDO::getStatus, status));
        return orders.stream()
                .map(OrdersDO::getAmount)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
    }

    private Long countUsers(LocalDateTime begin, LocalDateTime end) {
        return userMapper.selectCount(Wrappers.lambdaQuery(UserDO.class)
                .gt(begin != null, UserDO::getCreateTime, begin)
                .lt(end != null, UserDO::getCreateTime, end));
    }
}
