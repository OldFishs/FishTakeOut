package com.fish.req;

import lombok.Data;

import java.io.Serializable;

@Data
public class PasswordEdit implements Serializable {

    //员工id
    private Long empId;

    //旧密码
    private String oldPassword;

    //新密码
    private String newPassword;

}
