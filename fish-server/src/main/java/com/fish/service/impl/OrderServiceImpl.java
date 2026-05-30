package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.constant.MessageConstant;
import com.fish.context.BaseContext;
import com.fish.dto.OrdersCancelDTO;
import com.fish.dto.OrdersConfirmDTO;
import com.fish.dto.OrdersPageQueryDTO;
import com.fish.dto.OrdersPaymentDTO;
import com.fish.dto.OrdersRejectionDTO;
import com.fish.dto.OrdersSubmitDTO;
import com.fish.entity.AddressBook;
import com.fish.entity.OrderDetail;
import com.fish.entity.Orders;
import com.fish.entity.ShoppingCart;
import com.fish.exception.AddressBookBusinessException;
import com.fish.exception.OrderBusinessException;
import com.fish.exception.ShoppingCartBusinessException;
import com.fish.mapper.AddressBookMapper;
import com.fish.mapper.OrderDetailMapper;
import com.fish.mapper.OrderMapper;
import com.fish.mapper.ShoppingCartMapper;
import com.fish.result.PageResult;
import com.fish.service.OrderService;
import com.fish.utils.WeChatPayUtil;
import com.fish.vo.OrderPaymentVO;
import com.fish.vo.OrderStatisticsVO;
import com.fish.vo.OrderSubmitVO;
import com.fish.vo.OrderVO;
import com.fish.websocket.WebSocketServer;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        AddressBook addressBook = addressBookMapper.selectById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> cartList = shoppingCartMapper.selectList(
                Wrappers.lambdaQuery(ShoppingCart.class).eq(ShoppingCart::getUserId, userId));
        if (cartList == null || cartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setAddress(addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        cartList.forEach(cart -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        });
        orderDetailList.forEach(orderDetailMapper::insert);

        shoppingCartMapper.delete(Wrappers.lambdaQuery(ShoppingCart.class).eq(ShoppingCart::getUserId, userId));

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        paySuccess(ordersPaymentDTO.getOrderNumber());
        return null;
    }

    @Override
    public void paySuccess(String outTradeNo) {
        Orders ordersDB = orderMapper.selectOne(
                Wrappers.lambdaQuery(Orders.class).eq(Orders::getNumber, outTradeNo));

        orderMapper.update(null, Wrappers.lambdaUpdate(Orders.class)
                .eq(Orders::getId, ordersDB.getId())
                .set(Orders::getStatus, Orders.TO_BE_CONFIRMED)
                .set(Orders::getPayStatus, Orders.PAID)
                .set(Orders::getCheckoutTime, LocalDateTime.now()));

        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号: " + outTradeNo);
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    @Override
    public PageResult<OrderVO> pageQueryByUser(int page, int pageSize, Integer status) {
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        orderMapper.pageQuery(pageInfo, ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(pageInfo.getRecords())) {
            for (Orders orders : pageInfo.getRecords()) {
                List<OrderDetail> orderDetails = orderDetailMapper.selectList(
                        Wrappers.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId, orders.getId()));
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult<>(pageInfo.getTotal(), list);
    }

    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.selectById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(
                Wrappers.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId, orders.getId()));
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    @Override
    public void userCancelById(Long id) throws Exception {
        Orders ordersDB = orderMapper.selectById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
        }

        orderMapper.update(null, Wrappers.lambdaUpdate(Orders.class)
                .eq(Orders::getId, ordersDB.getId())
                .set(Orders::getStatus, Orders.CANCELLED)
                .set(Orders::getCancelReason, "用户取消")
                .set(Orders::getCancelTime, LocalDateTime.now())
                .set(ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED), Orders::getPayStatus, Orders.REFUND));
    }

    @Override
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(
                Wrappers.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId, id));

        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartList.forEach(shoppingCartMapper::insert);
    }

    @Override
    public PageResult<OrderVO> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        orderMapper.pageQuery(page, ordersPageQueryDTO);
        List<OrderVO> orderVOList = getOrderVOList(page.getRecords());
        return new PageResult<>(page.getTotal(), orderVOList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = Math.toIntExact(orderMapper.selectCount(
                Wrappers.lambdaQuery(Orders.class).eq(Orders::getStatus, Orders.TO_BE_CONFIRMED)));
        Integer confirmed = Math.toIntExact(orderMapper.selectCount(
                Wrappers.lambdaQuery(Orders.class).eq(Orders::getStatus, Orders.CONFIRMED)));
        Integer deliveryInProgress = Math.toIntExact(orderMapper.selectCount(
                Wrappers.lambdaQuery(Orders.class).eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)));

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        orderMapper.update(null, Wrappers.lambdaUpdate(Orders.class)
                .eq(Orders::getId, ordersConfirmDTO.getId())
                .set(Orders::getStatus, Orders.CONFIRMED));
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders ordersDB = orderMapper.selectById(ordersRejectionDTO.getId());
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orderMapper.update(null, Wrappers.lambdaUpdate(Orders.class)
                .eq(Orders::getId, ordersDB.getId())
                .set(Orders::getStatus, Orders.CANCELLED)
                .set(Orders::getRejectionReason, ordersRejectionDTO.getRejectionReason())
                .set(Orders::getCancelTime, LocalDateTime.now()));
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders ordersDB = orderMapper.selectById(ordersCancelDTO.getId());
        if (ordersDB.getPayStatus() == 1) {
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        orderMapper.update(null, Wrappers.lambdaUpdate(Orders.class)
                .eq(Orders::getId, ordersCancelDTO.getId())
                .set(Orders::getStatus, Orders.CANCELLED)
                .set(Orders::getCancelReason, ordersCancelDTO.getCancelReason())
                .set(Orders::getCancelTime, LocalDateTime.now()));
    }

    @Override
    public void delivery(Long id) {
        Orders ordersDB = orderMapper.selectById(id);
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orderMapper.update(null, Wrappers.lambdaUpdate(Orders.class)
                .eq(Orders::getId, ordersDB.getId())
                .set(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS));
    }

    @Override
    public void complete(Long id) {
        Orders ordersDB = orderMapper.selectById(id);
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orderMapper.update(null, Wrappers.lambdaUpdate(Orders.class)
                .eq(Orders::getId, ordersDB.getId())
                .set(Orders::getStatus, Orders.COMPLETED)
                .set(Orders::getDeliveryTime, LocalDateTime.now()));
    }

    @Override
    public void reminder(Long id) {
        Orders orders = orderMapper.selectById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    private List<OrderVO> getOrderVOList(List<Orders> ordersList) {
        List<OrderVO> orderVOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDishes(getOrderDishesStr(orders));
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    private String getOrderDishesStr(Orders orders) {
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(
                Wrappers.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId, orders.getId()));
        List<String> orderDishList = orderDetailList.stream()
                .map(x -> x.getName() + "*" + x.getNumber() + ";")
                .collect(Collectors.toList());
        return String.join("", orderDishList);
    }
}
