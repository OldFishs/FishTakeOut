package com.fish.service;

import com.fish.req.Employee;
import com.fish.req.EmployeeLogin;
import com.fish.req.EmployeePageQuery;
import com.fish.entity.EmployeeDO;
import com.fish.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    EmployeeDO login(EmployeeLogin employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     */
    void save(Employee employeeDTO);

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    PageResult pageQuery(EmployeePageQuery employeePageQueryDTO);

    /**
     * 启用禁用员工账号
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据id查询员工
     * @param id
     * @return
     */
    EmployeeDO getById(Long id);

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    void update(Employee employeeDTO);
}
