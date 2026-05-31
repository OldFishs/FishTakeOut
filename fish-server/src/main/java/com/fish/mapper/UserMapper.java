package com.fish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fish.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
