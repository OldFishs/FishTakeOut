package com.fish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fish.entity.AddressBook;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddressBookMapper extends BaseMapper<AddressBook> {
}
