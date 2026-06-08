#!/bin/bash
# 性能测试执行脚本
# 需安装 k6: https://k6.io/docs/getting-started/installation/
# 或使用 Docker: docker run --rm -i grafana/k6 run - < scripts/performance-test.js

BASE_URL="${BASE_URL:-http://localhost:8080/api}"
echo "性能测试: BASE_URL=$BASE_URL"
echo "模拟 1000+ 并发用户，目标响应时间 ≤3 秒"
echo "---"

if command -v k6 &> /dev/null; then
  k6 run --env BASE_URL="$BASE_URL" scripts/performance-test.js
else
  echo "未安装 k6，使用 Docker 运行:"
  echo "  docker run --rm -i --network host -v \$(pwd):/scripts grafana/k6 run /scripts/scripts/performance-test.js --env BASE_URL=$BASE_URL"
  docker run --rm -i --network host -v "$(pwd):/app" -w /app grafana/k6 run scripts/performance-test.js --env BASE_URL="$BASE_URL" 2>/dev/null || echo "请先安装 k6 或确保 Docker 可用"
fi
