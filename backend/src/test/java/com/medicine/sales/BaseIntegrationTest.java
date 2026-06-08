package com.medicine.sales;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.medicine.sales.dto.LoginDTO;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

/**
 * 集成测试基类，提供登录获取 Token 等公共方法
 */
public abstract class BaseIntegrationTest {

    protected String getAdminToken(TestRestTemplate restTemplate, String baseUrl) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("admin123");
        dto.setRole("ADMIN");
        ResponseEntity<String> resp = restTemplate.postForEntity(baseUrl + "/auth/login", dto, String.class);
        return extractToken(resp.getBody());
    }

    protected String getPurchaserToken(TestRestTemplate restTemplate, String baseUrl) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("buyer001");
        dto.setPassword("purchaser123");
        dto.setRole("PURCHASER");
        ResponseEntity<String> resp = restTemplate.postForEntity(baseUrl + "/auth/login", dto, String.class);
        return extractToken(resp.getBody());
    }

    protected String getMerchantToken(TestRestTemplate restTemplate, String baseUrl) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("tongrentang");
        dto.setPassword("merchant123");
        dto.setRole("MERCHANT");
        ResponseEntity<String> resp = restTemplate.postForEntity(baseUrl + "/auth/login", dto, String.class);
        return extractToken(resp.getBody());
    }

    protected String extractToken(String body) {
        if (body == null) return null;
        JSONObject json = JSON.parseObject(body);
        if (json.getInteger("code") != 200) return null;
        JSONObject data = json.getJSONObject("data");
        return data != null ? data.getString("token") : null;
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }
}
