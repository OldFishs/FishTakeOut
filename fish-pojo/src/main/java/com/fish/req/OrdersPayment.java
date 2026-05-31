package com.fish.req;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrdersPayment implements Serializable {
    //订单号
    private String orderNumber;

    //付款方式
    private Integer payMethod;

}
