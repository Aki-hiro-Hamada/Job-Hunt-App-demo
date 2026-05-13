#!/usr/bin/env bash
# ローカルで MongoDB を立ち上げてから Spring Boot を起動します。
# 使い方: ./run-local.sh   （Java 17 + Docker が必要）
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

if command -v docker >/dev/null 2>&1; then
  docker compose up -d
  echo "MongoDB の準備を待っています..."
  attempts=0
  while [ "$attempts" -lt 60 ]; do
    if docker compose exec -T mongo mongosh --quiet --eval 'quit(db.runCommand({ ping: 1 }).ok ? 0 : 1)' 2>/dev/null; then
      break
    fi
    attempts=$((attempts + 1))
    sleep 1
  done
  if [ "$attempts" -ge 60 ]; then
    echo "MongoDB が起動しませんでした。docker compose logs mongo を確認してください。" >&2
    exit 1
  fi
  echo "MongoDB OK"
else
  echo "注意: docker が PATH にありません。MongoDB を別途 localhost:27017 で起動している前提で続行します。" >&2
fi

echo "アプリを http://localhost:${PORT:-8081}/ で起動します（未登録なら /register からユーザー登録）。"
exec ./mvnw spring-boot:run "$@"
