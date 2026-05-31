package com.fish.resp;

import lombok.Data;


@Data
public class OrderStatisticsVO extends SimpleBaseVO {
    //待接单数量
    private Integer toBeConfirmed;

    //待派送数量
    private Integer confirmed;

    //派送中数量
    private Integer deliveryInProgress;
}
