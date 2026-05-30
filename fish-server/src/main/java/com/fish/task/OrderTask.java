package com.fish.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fish.entity.Orders;
import com.fish.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * ? ")
    public void processTimeoutOrders() {
        log.info("处理超时订单");
        List<Orders> orderList = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, Orders.PENDING_PAYMENT)
                .lt(Orders::getOrderTime, LocalDateTime.now().plusMinutes(-15)));

        if (orderList != null && !orderList.isEmpty()) {
            orderList.forEach(order -> orderMapper.update(null, Wrappers.lambdaUpdate(Orders.class)
                    .eq(Orders::getId, order.getId())
                    .set(Orders::getStatus, Orders.CANCELLED)
                    .set(Orders::getCancelReason, "订单超时，自动取消")
                    .set(Orders::getCancelTime, LocalDateTime.now())));
        }
    }

    @Scheduled(cron = "0 0 1 * * ? ")
    public void processDeliveryOrders() {
        log.info("处理派送中的订单");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> orderList = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
                .lt(Orders::getOrderTime, time));

        if (orderList != null && !orderList.isEmpty()) {
            orderList.forEach(order -> orderMapper.update(null, Wrappers.lambdaUpdate(Orders.class)
                    .eq(Orders::getId, order.getId())
                    .set(Orders::getStatus, Orders.COMPLETED)
                    .set(Orders::getDeliveryTime, LocalDateTime.now())));
        }
    }
}
