package com.medicine.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.sales.common.Constants;
import com.medicine.sales.entity.CartInfo;
import com.medicine.sales.entity.MedicineInfo;
import com.medicine.sales.exception.BusinessException;
import com.medicine.sales.mapper.CartMapper;
import com.medicine.sales.mapper.MedicineMapper;
import com.medicine.sales.service.CartService;
import com.medicine.sales.vo.CartVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Resource
    private CartMapper cartMapper;
    @Resource
    private MedicineMapper medicineMapper;

    @Override
    public List<CartVO> list(Long purchaserId) {
        return cartMapper.selectCartList(purchaserId);
    }

    @Override
    public void add(Long purchaserId, Long medicineId, Integer quantity) {
        MedicineInfo medicine = medicineMapper.selectById(medicineId);
        if (medicine == null || medicine.getStatus() != Constants.MEDICINE_STATUS_ON_SHELF) {
            throw new BusinessException("药材不存在或已下架");
        }

        CartInfo existing = cartMapper.selectOne(
                new LambdaQueryWrapper<CartInfo>()
                        .eq(CartInfo::getPurchaserId, purchaserId)
                        .eq(CartInfo::getMedicineId, medicineId));
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + (quantity != null ? quantity : 1));
            cartMapper.updateById(existing);
        } else {
            CartInfo cart = new CartInfo();
            cart.setPurchaserId(purchaserId);
            cart.setMedicineId(medicineId);
            cart.setMerchantId(medicine.getMerchantId());
            cart.setQuantity(quantity != null ? quantity : 1);
            cartMapper.insert(cart);
        }
        log.info("Cart updated: purchaserId={}, medicineId={}", purchaserId, medicineId);
    }

    @Override
    public void updateQuantity(Long cartId, Integer quantity, Long purchaserId) {
        CartInfo cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getPurchaserId().equals(purchaserId)) {
            throw new BusinessException("购物车项不存在");
        }
        if (quantity <= 0) {
            cartMapper.deleteById(cartId);
        } else {
            cart.setQuantity(quantity);
            cartMapper.updateById(cart);
        }
    }

    @Override
    public void delete(Long cartId, Long purchaserId) {
        CartInfo cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getPurchaserId().equals(purchaserId)) {
            throw new BusinessException("购物车项不存在");
        }
        cartMapper.deleteById(cartId);
    }

    @Override
    public void clear(Long purchaserId) {
        cartMapper.delete(new LambdaQueryWrapper<CartInfo>().eq(CartInfo::getPurchaserId, purchaserId));
    }
}
