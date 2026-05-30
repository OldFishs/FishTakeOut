package com.fish.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.dto.EmployeePageQueryDTO;
import com.fish.entity.Employee;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    default Page<Employee> pageQuery(Page<Employee> page, EmployeePageQueryDTO dto) {
        return selectPage(page, Wrappers.lambdaQuery(Employee.class)
                .like(StringUtils.isNotBlank(dto.getName()), Employee::getName, dto.getName())
                .orderByDesc(Employee::getCreateTime));
    }
}
