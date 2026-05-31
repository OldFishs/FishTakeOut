package com.fish.req;

import lombok.Data;

import java.io.Serializable;

@Data
public class ShoppingCart implements Serializable {

    private Long dishId;
    private Long setmealId;
    private String dishFlavor;

}
