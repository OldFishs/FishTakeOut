package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.constant.MessageConstant;
import com.fish.constant.PasswordConstant;
import com.fish.constant.StatusConstant;
import com.fish.context.BaseContext;
import com.fish.req.Employee;
import com.fish.req.EmployeeLogin;
import com.fish.req.EmployeePageQuery;
import com.fish.entity.EmployeeDO;
import com.fish.exception.AccountLockedException;
import com.fish.exception.AccountNotFoundException;
import com.fish.exception.PasswordErrorException;
import com.fish.mapper.EmployeeMapper;
import com.fish.result.PageResult;
import com.fish.service.EmployeeService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    public EmployeeDO login(EmployeeLogin employeeLoginDTO) {
        EmployeeDO employee = employeeMapper.selectOne(
                Wrappers.lambdaQuery(EmployeeDO.class)
                        .eq(EmployeeDO::getUsername, employeeLoginDTO.getUsername()));

        if (employee == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        String md5 = DigestUtils.md5DigestAsHex(employeeLoginDTO.getPassword().getBytes());
        if (!md5.equals(employee.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (Objects.equals(employee.getStatus(), StatusConstant.DISABLE)) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        return employee;
    }

    @Override
    public void save(Employee employeeDTO) {
        EmployeeDO employee = new EmployeeDO();
        BeanUtils.copyProperties(employeeDTO, employee);
        employee.setStatus(StatusConstant.ENABLE);
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        LocalDateTime now = LocalDateTime.now();
        employee.setCreateTime(now);
        employee.setUpdateTime(now);
        Long id = BaseContext.getCurrentId();
        employee.setCreateUser(id);
        employee.setUpdateUser(id);

        employeeMapper.insert(employee);
    }

    @Override
    public PageResult pageQuery(EmployeePageQuery employeePageQueryDTO) {
        Page<EmployeeDO> page = new Page<>(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        page = employeeMapper.pageQuery(page, employeePageQueryDTO);
        return new PageResult(page.getTotal(), page.getRecords());
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        employeeMapper.update(null, Wrappers.lambdaUpdate(EmployeeDO.class)
                .eq(EmployeeDO::getId, id)
                .set(EmployeeDO::getStatus, status)
                .set(EmployeeDO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public EmployeeDO getById(Long id) {
        return employeeMapper.selectById(id);
    }

    @Override
    public void update(Employee employeeDTO) {
        EmployeeDO employee = new EmployeeDO();
        BeanUtils.copyProperties(employeeDTO, employee);
        employeeMapper.update(null, Wrappers.lambdaUpdate(EmployeeDO.class)
                .eq(EmployeeDO::getId, employee.getId())
                .set(StringUtils.isNotBlank(employee.getName()), EmployeeDO::getName, employee.getName())
                .set(StringUtils.isNotBlank(employee.getUsername()), EmployeeDO::getUsername, employee.getUsername())
                .set(StringUtils.isNotBlank(employee.getPassword()), EmployeeDO::getPassword, employee.getPassword())
                .set(StringUtils.isNotBlank(employee.getPhone()), EmployeeDO::getPhone, employee.getPhone())
                .set(StringUtils.isNotBlank(employee.getSex()), EmployeeDO::getSex, employee.getSex())
                .set(StringUtils.isNotBlank(employee.getIdNumber()), EmployeeDO::getIdNumber, employee.getIdNumber())
                .set(employee.getStatus() != null, EmployeeDO::getStatus, employee.getStatus())
                .set(EmployeeDO::getUpdateTime, LocalDateTime.now())
                .set(EmployeeDO::getUpdateUser, BaseContext.getCurrentId()));
    }
}
