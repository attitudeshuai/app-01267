package com.medicine.sales.controller;

import com.medicine.sales.common.Result;
import com.medicine.sales.dto.LoginDTO;
import com.medicine.sales.dto.MerchantRegisterDTO;
import com.medicine.sales.dto.PurchaserRegisterDTO;
import com.medicine.sales.service.AuthService;
import com.medicine.sales.vo.LoginVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/register/purchaser")
    public Result<Void> registerPurchaser(@Valid @RequestBody PurchaserRegisterDTO dto) {
        authService.registerPurchaser(dto);
        return Result.success();
    }

    @PostMapping("/register/merchant")
    public Result<Void> registerMerchant(@Valid @RequestBody MerchantRegisterDTO dto) {
        authService.registerMerchant(dto);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }
}
