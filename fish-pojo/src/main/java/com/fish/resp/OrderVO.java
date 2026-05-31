package com.fish.resp;

import com.fish.entity.OrderDetailDO;
import com.fish.entity.OrdersDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO extends OrdersDO {

    //订单菜品信息
    private String orderDishes;

    //订单详情
    private List<OrderDetailDO> orderDetailList;

}
