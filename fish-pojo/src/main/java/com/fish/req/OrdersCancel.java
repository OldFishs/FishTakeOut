package com.fish.req;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrdersCancel implements Serializable {

    private Long id;
    //订单取消原因
    private String cancelReason;

}
