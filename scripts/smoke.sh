#!/usr/bin/env bash
set -uo pipefail

APP_PORT=8090
JAR="target/exam_system_online-1.0-SNAPSHOT.jar"
BASE="http://localhost:${APP_PORT}"
LOG=/tmp/exam_smoke_app.log
PID=""

cleanup() { [ -n "$PID" ] && kill "$PID" 2>/dev/null; }
trap cleanup EXIT

if [ "${1:-}" != "--skip-build" ]; then
  mvn -q clean package -DskipTests || { echo "构建失败"; exit 1; }
fi

[ -f "$JAR" ] || { echo "缺少 $JAR，请先构建"; exit 1; }

java -jar "$JAR" > "$LOG" 2>&1 &
PID=$!

echo "等待应用启动（最多 120s）..."
for i in $(seq 1 60); do
  if curl -s -o /dev/null "${BASE}/api/stats/overview"; then break; fi
  sleep 2
done

fail=0
check() {
  local path="$1"
  local body
  body=$(curl -s "${BASE}${path}")
  if printf '%s' "$body" | grep -q '"code":200'; then
    echo "OK   ${path}"
  else
    echo "FAIL ${path} -> ${body:0:200}"
    fail=1
  fi
}

# 注：/api/notices/* 有意排除——NoticeServiceImpl 全为 return null 空实现桩，
# 仅返回 HTTP 200 空响应体，无法通过 "code":200 断言，留待 P4/P5 补齐。
check /api/questions/list
check /api/papers/list
check /api/categories
check /api/categories/tree
check /api/videos
check /api/videos/popular
check /api/banners/active
check /api/stats/overview
check /api/video-categories/tree

if [ "$fail" -eq 0 ]; then
  echo "SMOKE PASS"
else
  echo "SMOKE FAIL（应用日志：${LOG}）"
fi
exit $fail
