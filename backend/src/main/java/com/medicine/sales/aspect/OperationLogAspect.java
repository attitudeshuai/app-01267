package com.medicine.sales.aspect;

import com.alibaba.fastjson.JSON;
import com.medicine.sales.common.UserContext;
import com.medicine.sales.entity.OperationLog;
import com.medicine.sales.mapper.OperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Resource
    private OperationLogMapper operationLogMapper;

    @Pointcut("execution(* com.medicine.sales.controller..*.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        String httpMethod = getHttpMethod(method);
        if (httpMethod != null && !"GET".equals(httpMethod)) {
            String className = point.getTarget().getClass().getSimpleName();
            String methodName = method.getName();
            Long userId = UserContext.getUserId();
            String role = UserContext.getRole();
            log.info("[Operation] user={}, role={}, {}.{}, method={}",
                    userId, role, className, methodName, httpMethod);
            saveOperationLog(userId, role, className, methodName, httpMethod, point);
        }

        Object result = point.proceed();

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > 3000) {
            log.warn("[SlowAPI] {}.{} took {}ms", point.getTarget().getClass().getSimpleName(),
                    method.getName(), elapsed);
        }

        return result;
    }

    private void saveOperationLog(Long userId, String role, String controller, String method, String httpMethod, ProceedingJoinPoint point) {
        try {
            if (userId == null) return;
            OperationLog logEntity = new OperationLog();
            logEntity.setOperatorType(roleToType(role));
            logEntity.setOperatorId(userId);
            logEntity.setOperatorName(UserContext.get() != null ? UserContext.get().getUsername() : null);
            logEntity.setModule(controller.replace("Controller", ""));
            logEntity.setAction(httpMethod + " " + method);
            logEntity.setTargetType(null);
            logEntity.setTargetId(null);
            Map<String, Object> detail = new HashMap<>();
            detail.put("controller", controller);
            detail.put("method", method);
            Object[] args = point.getArgs();
            if (args != null && args.length > 0 && args[0] != null && !(args[0] instanceof HttpServletRequest)) {
                try {
                    detail.put("args", args[0].toString());
                } catch (Exception ignored) {}
            }
            logEntity.setDetail(JSON.toJSONString(detail));
            logEntity.setIp(getClientIp());
            operationLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("Failed to save operation log: {}", e.getMessage());
        }
    }

    private int roleToType(String role) {
        if (role == null) return 1;
        switch (role) {
            case "admin": return 1;
            case "merchant": return 2;
            case "purchaser": return 3;
            default: return 1;
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                if (req != null) {
                    String ip = req.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty()) ip = req.getHeader("X-Real-IP");
                    if (ip == null || ip.isEmpty()) ip = req.getRemoteAddr();
                    return ip != null ? ip.split(",")[0].trim() : null;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getHttpMethod(Method method) {
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        RequestMapping rm = method.getAnnotation(RequestMapping.class);
        if (rm != null && rm.method().length > 0) return rm.method()[0].name();
        return null;
    }
}
