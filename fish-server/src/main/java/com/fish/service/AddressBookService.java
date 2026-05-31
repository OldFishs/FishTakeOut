package com.fish.service;

import com.fish.entity.AddressBookDO;

import java.util.List;

public interface AddressBookService {

    List<AddressBookDO> list(AddressBookDO addressBook);

    void save(AddressBookDO addressBook);

    AddressBookDO getById(Long id);

    void update(AddressBookDO addressBook);

    void setDefault(AddressBookDO addressBook);

    void deleteById(Long id);

}
