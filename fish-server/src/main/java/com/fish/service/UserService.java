package com.fish.service;

import com.fish.req.UserLogin;
import com.fish.entity.UserDO;

public interface UserService {

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    UserDO wxLogin(UserLogin userLoginDTO);
}
