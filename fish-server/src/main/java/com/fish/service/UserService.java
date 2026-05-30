package com.fish.service;

import com.fish.dto.UserLoginDTO;
import com.fish.entity.User;

public interface UserService {

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
