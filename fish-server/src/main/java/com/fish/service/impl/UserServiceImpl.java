package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fish.constant.MessageConstant;
import com.fish.req.UserLogin;
import com.fish.entity.UserDO;
import com.fish.exception.LoginFailedException;
import com.fish.mapper.UserMapper;
import com.fish.properties.WeChatProperties;
import com.fish.service.UserService;
import com.fish.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDO wxLogin(UserLogin userLoginDTO) {
        String openid = getOpenId(userLoginDTO.getCode());
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        UserDO user = userMapper.selectOne(Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getOpenid, openid));
        if (user == null) {
            user = UserDO.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        return user;
    }

    private String getOpenId(String code) {
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("appid", weChatProperties.getAppid());
        reqParams.put("secret", weChatProperties.getSecret());
        reqParams.put("js_code", code);
        reqParams.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, reqParams);
        JSONObject parseJson = JSON.parseObject(json);
        return parseJson.getString("openid");
    }
}
