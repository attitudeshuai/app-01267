package com.medicine.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.sales.common.Constants;
import com.medicine.sales.common.UserContext;
import com.medicine.sales.dto.LoginDTO;
import com.medicine.sales.dto.MerchantRegisterDTO;
import com.medicine.sales.dto.PurchaserRegisterDTO;
import com.medicine.sales.entity.AdminInfo;
import com.medicine.sales.entity.MerchantInfo;
import com.medicine.sales.entity.PurchaserInfo;
import com.medicine.sales.exception.BusinessException;

import java.util.regex.Pattern;
import com.medicine.sales.mapper.AdminMapper;
import com.medicine.sales.mapper.MerchantMapper;
import com.medicine.sales.mapper.PurchaserMapper;
import com.medicine.sales.service.AuthService;
import com.medicine.sales.util.JwtUtil;
import com.medicine.sales.util.PasswordUtil;
import com.medicine.sales.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private AdminMapper adminMapper;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private PurchaserMapper purchaserMapper;
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public LoginVO login(LoginDTO dto) {
        LoginVO vo = new LoginVO();
        String role = dto.getRole().toUpperCase();

        switch (role) {
            case Constants.ROLE_ADMIN: {
                AdminInfo admin = adminMapper.selectOne(
                        new LambdaQueryWrapper<AdminInfo>().eq(AdminInfo::getUsername, dto.getUsername()));
                if (admin == null || !PasswordUtil.matches(dto.getPassword(), admin.getPassword())) {
                    throw new BusinessException("用户名或密码错误");
                }
                if (admin.getStatus() != Constants.STATUS_ENABLED) {
                    throw new BusinessException("账号已被禁用");
                }
                vo.setUserId(admin.getId());
                vo.setUsername(admin.getUsername());
                vo.setNickname(admin.getNickname());
                vo.setAvatar(admin.getAvatar());
                break;
            }
            case Constants.ROLE_MERCHANT: {
                MerchantInfo merchant = merchantMapper.selectOne(
                        new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getUsername, dto.getUsername()));
                if (merchant == null || !PasswordUtil.matches(dto.getPassword(), merchant.getPassword())) {
                    throw new BusinessException("用户名或密码错误");
                }
                if (merchant.getStatus() == Constants.MERCHANT_STATUS_PENDING) {
                    throw new BusinessException("账号正在审核中，请耐心等待");
                }
                if (merchant.getStatus() == Constants.MERCHANT_STATUS_REJECTED) {
                    throw new BusinessException("账号审核未通过，请联系管理员");
                }
                if (merchant.getStatus() == Constants.MERCHANT_STATUS_DISABLED) {
                    throw new BusinessException("账号已被禁用，请联系管理员");
                }
                vo.setUserId(merchant.getId());
                vo.setUsername(merchant.getUsername());
                vo.setNickname(merchant.getCompanyName());
                vo.setAvatar(merchant.getAvatar());
                break;
            }
            case Constants.ROLE_PURCHASER: {
                PurchaserInfo purchaser = purchaserMapper.selectOne(
                        new LambdaQueryWrapper<PurchaserInfo>().eq(PurchaserInfo::getUsername, dto.getUsername()));
                if (purchaser == null || !PasswordUtil.matches(dto.getPassword(), purchaser.getPassword())) {
                    throw new BusinessException("用户名或密码错误");
                }
                if (purchaser.getStatus() != Constants.STATUS_ENABLED) {
                    throw new BusinessException("账号已被禁用");
                }
                vo.setUserId(purchaser.getId());
                vo.setUsername(purchaser.getUsername());
                vo.setNickname(purchaser.getNickname());
                vo.setAvatar(purchaser.getAvatar());
                break;
            }
            default:
                throw new BusinessException("无效的用户角色");
        }

        String token = jwtUtil.generateToken(vo.getUserId(), vo.getUsername(), role);
        vo.setRole(role);
        vo.setToken(token);

        String redisKey = Constants.REDIS_TOKEN_PREFIX + vo.getUserId();
        redisTemplate.opsForValue().set(redisKey, token, 24, TimeUnit.HOURS);

        log.info("User login success: username={}, role={}", dto.getUsername(), role);
        return vo;
    }

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    @Override
    public void registerPurchaser(PurchaserRegisterDTO dto) {
        if (!USERNAME_PATTERN.matcher(dto.getUsername()).matches()) {
            throw new BusinessException("用户名只能包含字母、数字和下划线，不能有中文或特殊字符");
        }
        Long count = purchaserMapper.selectCount(
                new LambdaQueryWrapper<PurchaserInfo>().eq(PurchaserInfo::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        PurchaserInfo purchaser = new PurchaserInfo();
        purchaser.setUsername(dto.getUsername());
        purchaser.setPassword(PasswordUtil.encode(dto.getPassword()));
        purchaser.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        purchaser.setPhone(dto.getPhone());
        purchaser.setEmail(dto.getEmail());
        purchaser.setStatus(Constants.STATUS_ENABLED);
        purchaserMapper.insert(purchaser);
        log.info("Purchaser registered: {}", dto.getUsername());
    }

    @Override
    public void registerMerchant(MerchantRegisterDTO dto) {
        if (!USERNAME_PATTERN.matcher(dto.getUsername()).matches()) {
            throw new BusinessException("用户名只能包含字母、数字和下划线，不能有中文或特殊字符");
        }
        Long count = merchantMapper.selectCount(
                new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        MerchantInfo merchant = new MerchantInfo();
        merchant.setUsername(dto.getUsername());
        merchant.setPassword(PasswordUtil.encode(dto.getPassword()));
        merchant.setCompanyName(dto.getCompanyName());
        merchant.setLicenseNo(dto.getLicenseNo());
        merchant.setLicenseImage(dto.getLicenseImage());
        merchant.setQualificationImage(dto.getQualificationImage());
        merchant.setContactPerson(dto.getContactPerson());
        merchant.setPhone(dto.getPhone());
        merchant.setEmail(dto.getEmail());
        merchant.setDescription(dto.getDescription());
        merchant.setStatus(Constants.MERCHANT_STATUS_PENDING);
        merchantMapper.insert(merchant);
        log.info("Merchant registered: {}", dto.getUsername());
    }

    @Override
    public void logout() {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            redisTemplate.delete(Constants.REDIS_TOKEN_PREFIX + userId);
            log.info("User logout: userId={}", userId);
        }
    }
}
