#!/usr/bin/env bash
set -uo pipefail

APP_PORT=8090
JAR="target/exam_system_online-1.0-SNAPSHOT.jar"
BASE="http://localhost:${APP_PORT}"
LOG=/tmp/exam_smoke_app.log
PID=""
# P2 起接口需要登录，冒烟用管理员账号；口令仅适用于本库的本地种子数据
SMOKE_USER="${SMOKE_USER:-admin}"
SMOKE_PASSWORD="${SMOKE_PASSWORD:-admin123}"

cleanup() { [ -n "$PID" ] && kill "$PID" 2>/dev/null; }
trap cleanup EXIT

if [ "${1:-}" != "--skip-build" ]; then
  mvn -q clean package -DskipTests || { echo "构建失败"; exit 1; }
fi

[ -f "$JAR" ] || { echo "缺少 $JAR，请先构建"; exit 1; }

# 本机只有 16G 且 swap 常年吃紧，限制堆与线程栈，避免冒烟把机器压卡
java -Xmx640m -Xss512k -XX:+UseSerialGC -jar "$JAR" > "$LOG" 2>&1 &
PID=$!

echo "等待应用启动（最多 120s）..."
for i in $(seq 1 60); do
  if curl -s -o /dev/null --connect-timeout 3 --max-time 10 "${BASE}/actuator/health"; then break; fi
  sleep 2
done

fail=0

# 先登录拿 access token，受保护接口都要带
TOKEN=$(curl -s --connect-timeout 3 --max-time 15 -X POST "${BASE}/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${SMOKE_USER}\",\"password\":\"${SMOKE_PASSWORD}\"}" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
  echo "FAIL /api/auth/login 未取得 token（见 ${LOG}）"
  exit 1
fi
echo "OK   /api/auth/login"

check() {
  local path="$1"
  local body
  body=$(curl -s --connect-timeout 3 --max-time 10 -H "Authorization: Bearer ${TOKEN}" "${BASE}${path}")
  if printf '%s' "$body" | grep -qE '"code":200([,}])'; then
    echo "OK   ${path}"
  else
    echo "FAIL ${path} -> ${body:0:200}"
    fail=1
  fi
}

# P2 鉴权断言：受保护接口匿名访问必须是 401，不能退化回放行
anonymous_denied() {
  local path="$1"
  local status
  status=$(curl -s -o /dev/null -w '%{http_code}' --connect-timeout 3 --max-time 10 "${BASE}${path}")
  if [ "$status" = "401" ]; then
    echo "OK   401 ${path}（匿名被拒）"
  else
    echo "FAIL ${path} 期望匿名 401，实际 ${status}"
    fail=1
  fi
}

# 注：/api/notices/* 有意排除——NoticeServiceImpl 全为 return null 空实现桩，
# 仅返回 HTTP 200 空响应体，无法通过 "code":200 断言，留待 P5 补齐。
# 另：/api/videos、/api/stats/overview 等仍是空实现桩（code 200 但 data null），
# 本脚本只断言 code，不校验 data，见总体计划 §9.5c。
check /api/questions/list
check /api/papers/list
check /api/categories
check /api/categories/tree
check /api/videos
check /api/videos/popular
check /api/banners/active
check /api/stats/overview
check /api/video-categories/tree
check /api/exam-records/list
check /api/auth/me

anonymous_denied /api/questions/list
anonymous_denied /api/exam-records/list

if [ "$fail" -eq 0 ]; then
  echo "SMOKE PASS"
else
  echo "SMOKE FAIL（应用日志：${LOG}）"
fi
exit $fail
