package com.fish.req;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrdersRejection implements Serializable {

    private Long id;

    //订单拒绝原因
    private String rejectionReason;

}
