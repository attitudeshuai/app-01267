/**
 * 性能测试脚本 - k6
 * 模拟 1000+ 并发用户，测试系统响应时间（要求≤3秒）、数据库吞吐量
 * 运行: k6 run scripts/performance-test.js
 * 安装 k6: https://k6.io/docs/getting-started/installation/
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '1m', target: 100 },
    { duration: '2m', target: 500 },
    { duration: '2m', target: 1000 },
    { duration: '2m', target: 1200 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    errors: ['rate<0.05'],
  },
};

function login(role) {
  const payload = JSON.stringify({
    username: role === 'admin' ? 'admin' : 'buyer001',
    password: role === 'admin' ? 'admin123' : 'purchaser123',
    role: role === 'admin' ? 'ADMIN' : 'PURCHASER',
  });
  const res = http.post(`${BASE_URL}/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  const ok = check(res, { 'login ok': (r) => r.status === 200 });
  if (!ok) errorRate.add(1);
  const body = JSON.parse(res.body);
  return body.data && body.data.token ? body.data.token : null;
}

export default function () {
  const token = login('purchaser');
  if (token) {
    const headers = { Authorization: `Bearer ${token}` };
    const res = http.get(`${BASE_URL}/purchaser/medicines/recommend`, { headers });
    const ok = check(res, {
      'recommend status 200': (r) => r.status === 200,
      'recommend duration < 3s': (r) => r.timings.duration < 3000,
    });
    if (!ok) errorRate.add(1);
  }

  const commonRes = http.get(`${BASE_URL}/common/medicines?current=1&size=10`);
  check(commonRes, {
    'common medicines status 200': (r) => r.status === 200,
    'common medicines duration < 3s': (r) => r.timings.duration < 3000,
  }) || errorRate.add(1);

  sleep(1);
}
