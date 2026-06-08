package com.medicine.sales.service;

import com.medicine.sales.dto.LoginDTO;
import com.medicine.sales.dto.MerchantRegisterDTO;
import com.medicine.sales.dto.PurchaserRegisterDTO;
import com.medicine.sales.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO dto);
    void registerPurchaser(PurchaserRegisterDTO dto);
    void registerMerchant(MerchantRegisterDTO dto);
    void logout();
}
