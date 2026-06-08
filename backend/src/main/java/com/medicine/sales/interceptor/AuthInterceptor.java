package com.medicine.sales.interceptor;

import com.medicine.sales.common.Constants;
import com.medicine.sales.common.UserContext;
import com.medicine.sales.exception.BusinessException;
import com.medicine.sales.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(Constants.TOKEN_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(Constants.TOKEN_PREFIX)) {
            throw new BusinessException(401, "Please login first");
        }

        String token = header.substring(Constants.TOKEN_PREFIX.length());
        try {
            if (jwtUtil.isTokenExpired(token)) {
                throw new BusinessException(401, "Token expired, please login again");
            }

            String redisKey = Constants.REDIS_TOKEN_PREFIX + jwtUtil.getUserId(token);
            Object cachedToken = redisTemplate.opsForValue().get(redisKey);
            if (cachedToken == null) {
                throw new BusinessException(401, "Token invalid, please login again");
            }

            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.get("userId").toString());
            String username = claims.get("username").toString();
            String role = claims.get("role").toString();

            UserContext.set(new UserContext.UserInfo(userId, username, role));
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token parse error: ", e);
            throw new BusinessException(401, "Invalid token");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
