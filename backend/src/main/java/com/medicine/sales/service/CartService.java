package com.medicine.sales.service;

import com.medicine.sales.vo.CartVO;
import java.util.List;

public interface CartService {
    List<CartVO> list(Long purchaserId);
    void add(Long purchaserId, Long medicineId, Integer quantity);
    void updateQuantity(Long cartId, Integer quantity, Long purchaserId);
    void delete(Long cartId, Long purchaserId);
    void clear(Long purchaserId);
}
