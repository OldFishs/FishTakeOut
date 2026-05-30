package com.fish.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fish.context.BaseContext;
import com.fish.entity.AddressBook;
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
    public List<AddressBook> list(AddressBook addressBook) {
        return addressBookMapper.selectList(Wrappers.lambdaQuery(AddressBook.class)
                .eq(addressBook.getUserId() != null, AddressBook::getUserId, addressBook.getUserId())
                .eq(addressBook.getPhone() != null, AddressBook::getPhone, addressBook.getPhone())
                .eq(addressBook.getIsDefault() != null, AddressBook::getIsDefault, addressBook.getIsDefault()));
    }

    @Override
    public void save(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    @Override
    public AddressBook getById(Long id) {
        return addressBookMapper.selectById(id);
    }

    @Override
    public void update(AddressBook addressBook) {
        addressBookMapper.update(null, Wrappers.lambdaUpdate(AddressBook.class)
                .eq(AddressBook::getId, addressBook.getId())
                .set(addressBook.getConsignee() != null, AddressBook::getConsignee, addressBook.getConsignee())
                .set(addressBook.getSex() != null, AddressBook::getSex, addressBook.getSex())
                .set(addressBook.getPhone() != null, AddressBook::getPhone, addressBook.getPhone())
                .set(addressBook.getDetail() != null, AddressBook::getDetail, addressBook.getDetail())
                .set(addressBook.getLabel() != null, AddressBook::getLabel, addressBook.getLabel())
                .set(addressBook.getIsDefault() != null, AddressBook::getIsDefault, addressBook.getIsDefault()));
    }

    @Override
    @Transactional
    public void setDefault(AddressBook addressBook) {
        addressBookMapper.update(null, Wrappers.lambdaUpdate(AddressBook.class)
                .eq(AddressBook::getUserId, BaseContext.getCurrentId())
                .set(AddressBook::getIsDefault, 0));

        addressBookMapper.update(null, Wrappers.lambdaUpdate(AddressBook.class)
                .eq(AddressBook::getId, addressBook.getId())
                .set(AddressBook::getIsDefault, 1));
    }

    @Override
    public void deleteById(Long id) {
        addressBookMapper.deleteById(id);
    }
}
