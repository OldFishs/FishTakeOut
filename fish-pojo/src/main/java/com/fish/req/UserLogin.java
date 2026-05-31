package com.fish.req;

import lombok.Data;

import java.io.Serializable;

/**
 * C端用户登录
 */
@Data
public class UserLogin implements Serializable {

    private String code;

}
