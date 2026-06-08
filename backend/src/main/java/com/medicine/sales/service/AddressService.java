package com.medicine.sales.service;

import com.medicine.sales.entity.AddressInfo;
import java.util.List;

public interface AddressService {
    List<AddressInfo> list(Long purchaserId);
    void create(AddressInfo address);
    void update(AddressInfo address, Long purchaserId);
    void delete(Long id, Long purchaserId);
    void setDefault(Long id, Long purchaserId);
}
