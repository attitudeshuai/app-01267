package com.medicine.sales.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.sales.entity.AdminInfo;
import com.medicine.sales.entity.MerchantInfo;
import com.medicine.sales.entity.PurchaserInfo;
import com.medicine.sales.mapper.AdminMapper;
import com.medicine.sales.mapper.MerchantMapper;
import com.medicine.sales.mapper.PurchaserMapper;
import com.medicine.sales.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private static final String MERCHANT_PASSWORD = "merchant123";
    private static final String PURCHASER_PASSWORD = "purchaser123";

    @Resource
    private AdminMapper adminMapper;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private PurchaserMapper purchaserMapper;

    @Override
    public void run(String... args) {
        initAdmin();
        initMerchantPasswords();
        initPurchaserPasswords();
    }

    private void initAdmin() {
        AdminInfo admin = adminMapper.selectOne(
                new LambdaQueryWrapper<AdminInfo>().eq(AdminInfo::getUsername, "admin"));

        if (admin == null) {
            admin = new AdminInfo();
            admin.setUsername("admin");
            admin.setPassword(PasswordUtil.encode("admin123"));
            admin.setNickname("System Admin");
            admin.setPhone("13800000000");
            admin.setStatus(1);
            adminMapper.insert(admin);
            log.info("Default admin account created: admin / admin123");
        } else if (admin.getPassword() == null || !admin.getPassword().startsWith("$2a$")) {
            admin.setPassword(PasswordUtil.encode("admin123"));
            adminMapper.updateById(admin);
            log.info("Admin password reset: admin / admin123");
        }
    }

    private void initMerchantPasswords() {
        List<String> defaultMerchants = Arrays.asList("tongrentang", "huqingyutang", "yunnanbaiyao");
        for (String username : defaultMerchants) {
            MerchantInfo m = merchantMapper.selectOne(
                    new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getUsername, username));
            if (m != null && !PasswordUtil.matches(MERCHANT_PASSWORD, m.getPassword())) {
                m.setPassword(PasswordUtil.encode(MERCHANT_PASSWORD));
                merchantMapper.updateById(m);
                log.info("Merchant password reset (hash mismatch): {} / {}", username, MERCHANT_PASSWORD);
            }
        }
    }

    private void initPurchaserPasswords() {
        List<String> defaultPurchasers = Arrays.asList("buyer001", "buyer002", "buyer003");
        for (String username : defaultPurchasers) {
            PurchaserInfo p = purchaserMapper.selectOne(
                    new LambdaQueryWrapper<PurchaserInfo>().eq(PurchaserInfo::getUsername, username));
            if (p != null && !PasswordUtil.matches(PURCHASER_PASSWORD, p.getPassword())) {
                p.setPassword(PasswordUtil.encode(PURCHASER_PASSWORD));
                purchaserMapper.updateById(p);
                log.info("Purchaser password reset (hash mismatch): {} / {}", username, PURCHASER_PASSWORD);
            }
        }
    }
}
