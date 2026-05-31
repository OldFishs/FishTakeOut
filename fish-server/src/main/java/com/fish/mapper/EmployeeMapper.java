package com.fish.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fish.req.EmployeePageQuery;
import com.fish.entity.EmployeeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<EmployeeDO> {

    default Page<EmployeeDO> pageQuery(Page<EmployeeDO> page, EmployeePageQuery dto) {
        return selectPage(page, Wrappers.lambdaQuery(EmployeeDO.class)
                .like(StringUtils.isNotBlank(dto.getName()), EmployeeDO::getName, dto.getName())
                .orderByDesc(EmployeeDO::getCreateTime));
    }
}
