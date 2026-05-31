package com.fish.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fish.entity.OrdersDO;
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
        List<OrdersDO> orderList = orderMapper.selectList(Wrappers.lambdaQuery(OrdersDO.class)
                .eq(OrdersDO::getStatus, OrdersDO.PENDING_PAYMENT)
                .lt(OrdersDO::getOrderTime, LocalDateTime.now().plusMinutes(-15)));

        if (orderList != null && !orderList.isEmpty()) {
            orderList.forEach(order -> orderMapper.update(null, Wrappers.lambdaUpdate(OrdersDO.class)
                    .eq(OrdersDO::getId, order.getId())
                    .set(OrdersDO::getStatus, OrdersDO.CANCELLED)
                    .set(OrdersDO::getCancelReason, "订单超时，自动取消")
                    .set(OrdersDO::getCancelTime, LocalDateTime.now())));
        }
    }

    @Scheduled(cron = "0 0 1 * * ? ")
    public void processDeliveryOrders() {
        log.info("处理派送中的订单");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<OrdersDO> orderList = orderMapper.selectList(Wrappers.lambdaQuery(OrdersDO.class)
                .eq(OrdersDO::getStatus, OrdersDO.DELIVERY_IN_PROGRESS)
                .lt(OrdersDO::getOrderTime, time));

        if (orderList != null && !orderList.isEmpty()) {
            orderList.forEach(order -> orderMapper.update(null, Wrappers.lambdaUpdate(OrdersDO.class)
                    .eq(OrdersDO::getId, order.getId())
                    .set(OrdersDO::getStatus, OrdersDO.COMPLETED)
                    .set(OrdersDO::getDeliveryTime, LocalDateTime.now())));
        }
    }
}
