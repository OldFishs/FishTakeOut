package com.fish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fish.entity.ShoppingCartDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCartDO> {
}
