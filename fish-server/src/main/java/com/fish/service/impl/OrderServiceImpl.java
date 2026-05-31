package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.constant.MessageConstant;
import com.fish.context.BaseContext;
import com.fish.req.OrdersCancel;
import com.fish.req.OrdersConfirm;
import com.fish.req.OrdersPageQuery;
import com.fish.req.OrdersPayment;
import com.fish.req.OrdersRejection;
import com.fish.req.OrdersSubmit;
import com.fish.entity.AddressBookDO;
import com.fish.entity.OrderDetailDO;
import com.fish.entity.OrdersDO;
import com.fish.entity.ShoppingCartDO;
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
import com.fish.resp.OrderPaymentVO;
import com.fish.resp.OrderStatisticsVO;
import com.fish.resp.OrderSubmitVO;
import com.fish.resp.OrderVO;
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
    public OrderSubmitVO submitOrder(OrdersSubmit ordersSubmitDTO) {
        AddressBookDO addressBook = addressBookMapper.selectById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        List<ShoppingCartDO> cartList = shoppingCartMapper.selectList(
                Wrappers.lambdaQuery(ShoppingCartDO.class).eq(ShoppingCartDO::getUserId, userId));
        if (cartList == null || cartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        OrdersDO orders = new OrdersDO();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(OrdersDO.UN_PAID);
        orders.setStatus(OrdersDO.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setAddress(addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orderMapper.insert(orders);

        List<OrderDetailDO> orderDetailList = new ArrayList<>();
        cartList.forEach(cart -> {
            OrderDetailDO orderDetail = new OrderDetailDO();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        });
        orderDetailList.forEach(orderDetailMapper::insert);

        shoppingCartMapper.delete(Wrappers.lambdaQuery(ShoppingCartDO.class).eq(ShoppingCartDO::getUserId, userId));

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    @Override
    public OrderPaymentVO payment(OrdersPayment ordersPaymentDTO) throws Exception {
        paySuccess(ordersPaymentDTO.getOrderNumber());
        return null;
    }

    @Override
    public void paySuccess(String outTradeNo) {
        OrdersDO ordersDB = orderMapper.selectOne(
                Wrappers.lambdaQuery(OrdersDO.class).eq(OrdersDO::getNumber, outTradeNo));

        orderMapper.update(null, Wrappers.lambdaUpdate(OrdersDO.class)
                .eq(OrdersDO::getId, ordersDB.getId())
                .set(OrdersDO::getStatus, OrdersDO.TO_BE_CONFIRMED)
                .set(OrdersDO::getPayStatus, OrdersDO.PAID)
                .set(OrdersDO::getCheckoutTime, LocalDateTime.now()));

        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号: " + outTradeNo);
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    @Override
    public PageResult<OrderVO> pageQueryByUser(int page, int pageSize, Integer status) {
        Page<OrdersDO> pageInfo = new Page<>(page, pageSize);
        OrdersPageQuery ordersPageQueryDTO = new OrdersPageQuery();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        orderMapper.pageQuery(pageInfo, ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(pageInfo.getRecords())) {
            for (OrdersDO orders : pageInfo.getRecords()) {
                List<OrderDetailDO> orderDetails = orderDetailMapper.selectList(
                        Wrappers.lambdaQuery(OrderDetailDO.class).eq(OrderDetailDO::getOrderId, orders.getId()));
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
        OrdersDO orders = orderMapper.selectById(id);
        List<OrderDetailDO> orderDetailList = orderDetailMapper.selectList(
                Wrappers.lambdaQuery(OrderDetailDO.class).eq(OrderDetailDO::getOrderId, orders.getId()));
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    @Override
    public void userCancelById(Long id) throws Exception {
        OrdersDO ordersDB = orderMapper.selectById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        if (ordersDB.getStatus().equals(OrdersDO.TO_BE_CONFIRMED)) {
            weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
        }

        orderMapper.update(null, Wrappers.lambdaUpdate(OrdersDO.class)
                .eq(OrdersDO::getId, ordersDB.getId())
                .set(OrdersDO::getStatus, OrdersDO.CANCELLED)
                .set(OrdersDO::getCancelReason, "用户取消")
                .set(OrdersDO::getCancelTime, LocalDateTime.now())
                .set(ordersDB.getStatus().equals(OrdersDO.TO_BE_CONFIRMED), OrdersDO::getPayStatus, OrdersDO.REFUND));
    }

    @Override
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        List<OrderDetailDO> orderDetailList = orderDetailMapper.selectList(
                Wrappers.lambdaQuery(OrderDetailDO.class).eq(OrderDetailDO::getOrderId, id));

        List<ShoppingCartDO> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCartDO shoppingCart = new ShoppingCartDO();
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartList.forEach(shoppingCartMapper::insert);
    }

    @Override
    public PageResult<OrderVO> conditionSearch(OrdersPageQuery ordersPageQueryDTO) {
        Page<OrdersDO> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        orderMapper.pageQuery(page, ordersPageQueryDTO);
        List<OrderVO> orderVOList = getOrderVOList(page.getRecords());
        return new PageResult<>(page.getTotal(), orderVOList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = Math.toIntExact(orderMapper.selectCount(
                Wrappers.lambdaQuery(OrdersDO.class).eq(OrdersDO::getStatus, OrdersDO.TO_BE_CONFIRMED)));
        Integer confirmed = Math.toIntExact(orderMapper.selectCount(
                Wrappers.lambdaQuery(OrdersDO.class).eq(OrdersDO::getStatus, OrdersDO.CONFIRMED)));
        Integer deliveryInProgress = Math.toIntExact(orderMapper.selectCount(
                Wrappers.lambdaQuery(OrdersDO.class).eq(OrdersDO::getStatus, OrdersDO.DELIVERY_IN_PROGRESS)));

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirm ordersConfirmDTO) {
        orderMapper.update(null, Wrappers.lambdaUpdate(OrdersDO.class)
                .eq(OrdersDO::getId, ordersConfirmDTO.getId())
                .set(OrdersDO::getStatus, OrdersDO.CONFIRMED));
    }

    @Override
    public void rejection(OrdersRejection ordersRejectionDTO) throws Exception {
        OrdersDO ordersDB = orderMapper.selectById(ordersRejectionDTO.getId());
        if (ordersDB == null || !ordersDB.getStatus().equals(OrdersDO.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orderMapper.update(null, Wrappers.lambdaUpdate(OrdersDO.class)
                .eq(OrdersDO::getId, ordersDB.getId())
                .set(OrdersDO::getStatus, OrdersDO.CANCELLED)
                .set(OrdersDO::getRejectionReason, ordersRejectionDTO.getRejectionReason())
                .set(OrdersDO::getCancelTime, LocalDateTime.now()));
    }

    @Override
    public void cancel(OrdersCancel ordersCancelDTO) throws Exception {
        OrdersDO ordersDB = orderMapper.selectById(ordersCancelDTO.getId());
        if (ordersDB.getPayStatus() == 1) {
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        orderMapper.update(null, Wrappers.lambdaUpdate(OrdersDO.class)
                .eq(OrdersDO::getId, ordersCancelDTO.getId())
                .set(OrdersDO::getStatus, OrdersDO.CANCELLED)
                .set(OrdersDO::getCancelReason, ordersCancelDTO.getCancelReason())
                .set(OrdersDO::getCancelTime, LocalDateTime.now()));
    }

    @Override
    public void delivery(Long id) {
        OrdersDO ordersDB = orderMapper.selectById(id);
        if (ordersDB == null || !ordersDB.getStatus().equals(OrdersDO.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orderMapper.update(null, Wrappers.lambdaUpdate(OrdersDO.class)
                .eq(OrdersDO::getId, ordersDB.getId())
                .set(OrdersDO::getStatus, OrdersDO.DELIVERY_IN_PROGRESS));
    }

    @Override
    public void complete(Long id) {
        OrdersDO ordersDB = orderMapper.selectById(id);
        if (ordersDB == null || !ordersDB.getStatus().equals(OrdersDO.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orderMapper.update(null, Wrappers.lambdaUpdate(OrdersDO.class)
                .eq(OrdersDO::getId, ordersDB.getId())
                .set(OrdersDO::getStatus, OrdersDO.COMPLETED)
                .set(OrdersDO::getDeliveryTime, LocalDateTime.now()));
    }

    @Override
    public void reminder(Long id) {
        OrdersDO orders = orderMapper.selectById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    private List<OrderVO> getOrderVOList(List<OrdersDO> ordersList) {
        List<OrderVO> orderVOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (OrdersDO orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDishes(getOrderDishesStr(orders));
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    private String getOrderDishesStr(OrdersDO orders) {
        List<OrderDetailDO> orderDetailList = orderDetailMapper.selectList(
                Wrappers.lambdaQuery(OrderDetailDO.class).eq(OrderDetailDO::getOrderId, orders.getId()));
        List<String> orderDishList = orderDetailList.stream()
                .map(x -> x.getName() + "*" + x.getNumber() + ";")
                .collect(Collectors.toList());
        return String.join("", orderDishList);
    }
}
