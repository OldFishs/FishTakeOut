package com.fish.req;

import lombok.Data;

import java.io.Serializable;

@Data
public class Employee implements Serializable {

    private Long id;

    private String username;

    private String name;

    private String phone;

    private String sex;

    private String idNumber;

}
