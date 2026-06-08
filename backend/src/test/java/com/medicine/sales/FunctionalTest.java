package com.medicine.sales;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 功能测试：验证各模块核心功能
 * 前置条件：MySQL、Redis 已启动，且已执行 schema.sql 初始化数据
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FunctionalTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
    }

    @Test
    @Order(1)
    @DisplayName("功能测试-登录：管理员登录成功")
    void testAdminLogin() {
        JSONObject dto = new JSONObject();
        dto.put("username", "admin");
        dto.put("password", "admin123");
        dto.put("role", "ADMIN");
        ResponseEntity<String> resp = restTemplate.postForEntity(baseUrl + "/auth/login", dto, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject body = JSON.parseObject(resp.getBody());
        assertEquals(200, body.getInteger("code"));
        assertNotNull(body.getJSONObject("data"));
        assertNotNull(body.getJSONObject("data").getString("token"));
    }

    @Test
    @Order(2)
    @DisplayName("功能测试-登录：采购商登录成功")
    void testPurchaserLogin() {
        JSONObject dto = new JSONObject();
        dto.put("username", "buyer001");
        dto.put("password", "purchaser123");
        dto.put("role", "PURCHASER");
        ResponseEntity<String> resp = restTemplate.postForEntity(baseUrl + "/auth/login", dto, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject body = JSON.parseObject(resp.getBody());
        assertEquals(200, body.getInteger("code"));
        assertNotNull(extractToken(resp.getBody()));
    }

    @Test
    @Order(3)
    @DisplayName("功能测试-商户入驻审核：管理员审核商户")
    void testMerchantAudit() {
        String token = getAdminToken(restTemplate, baseUrl);
        assertNotNull(token, "需先登录获取 Token");

        HttpHeaders headers = authHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/admin/merchants?current=1&size=10",
                HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject body = JSON.parseObject(resp.getBody());
        assertEquals(200, body.getInteger("code"));
        assertNotNull(body.getJSONObject("data"));
    }

    @Test
    @Order(4)
    @DisplayName("功能测试-订单创建：采购商创建订单")
    void testOrderCreate() {
        String token = getPurchaserToken(restTemplate, baseUrl);
        assertNotNull(token, "需先登录获取 Token");

        String orderJson = "{\"addressId\":1,\"items\":[{\"medicineId\":1,\"quantity\":10}]}";
        HttpHeaders headers = authHeaders(token);
        HttpEntity<String> entity = new HttpEntity<>(orderJson, headers);
        ResponseEntity<String> resp = restTemplate.exchange(baseUrl + "/purchaser/orders", HttpMethod.POST, entity, String.class);
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        JSONObject body = JSON.parseObject(resp.getBody());
        assertEquals(200, body.getInteger("code"));
        assertNotNull(body.get("data"));
    }

    @Test
    @Order(5)
    @DisplayName("功能测试-推荐算法：返回个性化推荐列表")
    void testRecommendAlgorithm() {
        String token = getPurchaserToken(restTemplate, baseUrl);
        assertNotNull(token, "需先登录获取 Token");

        HttpHeaders headers = authHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/purchaser/medicines/recommend",
                HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject body = JSON.parseObject(resp.getBody());
        assertEquals(200, body.getInteger("code"));
        assertNotNull(body.get("data"));
        assertTrue(body.getJSONArray("data") != null);
    }

    @Test
    @Order(6)
    @DisplayName("功能测试-公共接口：药材列表无需鉴权")
    void testCommonMedicinesNoAuth() {
        ResponseEntity<String> resp = restTemplate.getForEntity(baseUrl + "/common/medicines?current=1&size=10", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        JSONObject body = JSON.parseObject(resp.getBody());
        assertEquals(200, body.getInteger("code"));
    }
}
