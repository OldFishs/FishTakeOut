package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fish.context.BaseContext;
import com.fish.entity.AddressBookDO;
import com.fish.mapper.AddressBookMapper;
import com.fish.service.AddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Override
    public List<AddressBookDO> list(AddressBookDO addressBook) {
        return addressBookMapper.selectList(Wrappers.lambdaQuery(AddressBookDO.class)
                .eq(addressBook.getUserId() != null, AddressBookDO::getUserId, addressBook.getUserId())
                .eq(addressBook.getPhone() != null, AddressBookDO::getPhone, addressBook.getPhone())
                .eq(addressBook.getIsDefault() != null, AddressBookDO::getIsDefault, addressBook.getIsDefault()));
    }

    @Override
    public void save(AddressBookDO addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    @Override
    public AddressBookDO getById(Long id) {
        return addressBookMapper.selectById(id);
    }

    @Override
    public void update(AddressBookDO addressBook) {
        addressBookMapper.update(null, Wrappers.lambdaUpdate(AddressBookDO.class)
                .eq(AddressBookDO::getId, addressBook.getId())
                .set(addressBook.getConsignee() != null, AddressBookDO::getConsignee, addressBook.getConsignee())
                .set(addressBook.getSex() != null, AddressBookDO::getSex, addressBook.getSex())
                .set(addressBook.getPhone() != null, AddressBookDO::getPhone, addressBook.getPhone())
                .set(addressBook.getDetail() != null, AddressBookDO::getDetail, addressBook.getDetail())
                .set(addressBook.getLabel() != null, AddressBookDO::getLabel, addressBook.getLabel())
                .set(addressBook.getIsDefault() != null, AddressBookDO::getIsDefault, addressBook.getIsDefault()));
    }

    @Override
    @Transactional
    public void setDefault(AddressBookDO addressBook) {
        addressBookMapper.update(null, Wrappers.lambdaUpdate(AddressBookDO.class)
                .eq(AddressBookDO::getUserId, BaseContext.getCurrentId())
                .set(AddressBookDO::getIsDefault, 0));

        addressBookMapper.update(null, Wrappers.lambdaUpdate(AddressBookDO.class)
                .eq(AddressBookDO::getId, addressBook.getId())
                .set(AddressBookDO::getIsDefault, 1));
    }

    @Override
    public void deleteById(Long id) {
        addressBookMapper.deleteById(id);
    }
}
