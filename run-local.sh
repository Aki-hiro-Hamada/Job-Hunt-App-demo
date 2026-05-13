#!/usr/bin/env bash
# ローカルで Spring Boot を起動します。
#
# 既定: application.properties の Supabase 接続先を使用します。
#   事前に Supabase のパスワードを環境変数で指定:
#     export SUPABASE_DB_PASSWORD='＜Supabaseのパスワード＞'
#
# ローカルの Docker PostgreSQL を使いたい場合は --local-pg を付けます。
#   ./run-local.sh --local-pg
#
# 引数はそのまま spring-boot:run に渡せます（例: --spring-boot.run.arguments=--spring.profiles.active=dev）。
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

USE_LOCAL_PG=0
SB_ARGS=()
for arg in "$@"; do
  case "$arg" in
    --local-pg)
      USE_LOCAL_PG=1
      ;;
    *)
      SB_ARGS+=("$arg")
      ;;
  esac
done

if [ "$USE_LOCAL_PG" = "1" ]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "docker が PATH にありません。--local-pg を使うには Docker が必要です。" >&2
    exit 1
  fi
  docker compose up -d
  echo "PostgreSQL の準備を待っています..."
  attempts=0
  while [ "$attempts" -lt 60 ]; do
    if docker compose exec -T postgres pg_isready -U postgres >/dev/null 2>&1; then
      break
    fi
    attempts=$((attempts + 1))
    sleep 1
  done
  if [ "$attempts" -ge 60 ]; then
    echo "PostgreSQL が起動しませんでした。docker compose logs postgres を確認してください。" >&2
    exit 1
  fi
  echo "PostgreSQL OK"
  export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/job_hunting'
  export SPRING_DATASOURCE_USERNAME='postgres'
  export SUPABASE_DB_PASSWORD='postgres'
else
  if [ -z "${SUPABASE_DB_PASSWORD:-}" ]; then
    echo "環境変数 SUPABASE_DB_PASSWORD が未設定です。" >&2
    echo "  export SUPABASE_DB_PASSWORD='＜Supabaseのパスワード＞'" >&2
    echo "を実行してから再度起動してください。ローカルの Postgres を使う場合は --local-pg を付けてください。" >&2
    exit 1
  fi
  echo "Supabase に接続して起動します（application.properties の設定を使用）。"
fi

echo "アプリを http://localhost:${PORT:-8081}/ で起動します（未登録なら /register からユーザー登録）。"
exec ./mvnw spring-boot:run "${SB_ARGS[@]}"
