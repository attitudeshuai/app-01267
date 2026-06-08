package com.medicine.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.medicine.sales.entity.AddressInfo;
import com.medicine.sales.exception.BusinessException;
import com.medicine.sales.mapper.AddressMapper;
import com.medicine.sales.service.AddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class AddressServiceImpl implements AddressService {

    @Resource
    private AddressMapper addressMapper;

    @Override
    public List<AddressInfo> list(Long purchaserId) {
        return addressMapper.selectList(
                new LambdaQueryWrapper<AddressInfo>()
                        .eq(AddressInfo::getPurchaserId, purchaserId)
                        .orderByDesc(AddressInfo::getIsDefault)
                        .orderByDesc(AddressInfo::getCreatedTime));
    }

    @Override
    public void create(AddressInfo address) {
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(address.getPurchaserId());
        }
        addressMapper.insert(address);
        log.info("Address created: purchaserId={}", address.getPurchaserId());
    }

    @Override
    public void update(AddressInfo address, Long purchaserId) {
        AddressInfo existing = addressMapper.selectById(address.getId());
        if (existing == null || !existing.getPurchaserId().equals(purchaserId)) {
            throw new BusinessException("地址不存在或无权操作");
        }
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(purchaserId);
        }
        address.setPurchaserId(purchaserId);
        addressMapper.updateById(address);
    }

    @Override
    public void delete(Long id, Long purchaserId) {
        AddressInfo existing = addressMapper.selectById(id);
        if (existing == null || !existing.getPurchaserId().equals(purchaserId)) {
            throw new BusinessException("地址不存在或无权操作");
        }
        addressMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long id, Long purchaserId) {
        clearDefault(purchaserId);
        AddressInfo address = addressMapper.selectById(id);
        if (address == null || !address.getPurchaserId().equals(purchaserId)) {
            throw new BusinessException("地址不存在或无权操作");
        }
        address.setIsDefault(1);
        addressMapper.updateById(address);
    }

    private void clearDefault(Long purchaserId) {
        addressMapper.update(null,
                new LambdaUpdateWrapper<AddressInfo>()
                        .eq(AddressInfo::getPurchaserId, purchaserId)
                        .set(AddressInfo::getIsDefault, 0));
    }
}
