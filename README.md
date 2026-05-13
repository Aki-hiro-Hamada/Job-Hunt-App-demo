# Job-Hunt-App（内定ホイホイ）

応募先（企業）を登録し、選考ステータス・日付・メモ・履歴を管理するWebアプリです。

- 公開URL（デモ・採用共有用）: `https://job-hunt-app-demo.onrender.com`

## 制作の目的

- 転職・就職活動における応募先情報（企業名、選考ステータス、面接日、メモ、履歴など）を一か所で管理するため。
- Webアプリケーション開発の学習およびポートフォリオとして、設計・実装・クラウドデプロイまで一連を行うため。
- 採用担当者への共有時は個人の本番データと切り離すため、デモ用の公開URLとデータベースを別途用意する運用とするため。

## 主な機能

- 応募先の一覧表示
- 応募先の新規登録 / 編集 / 削除
- ステータス管理（応募前 / 書類選考中 / 面接… / 内定 / お見送り / 辞退）
- 過去履歴の追加・閲覧
- 旧URL互換（`/applications/new` → `/applications/create` に自動遷移）

## 画面（URL）

- 一覧: `/applications`
- 新規登録: `/applications/create`
- 詳細: `/applications/{id}`
- 編集: `/applications/edit/{id}`

## 技術スタック

- Java 17
- Spring Boot 3.2.x
- Thymeleaf
- MongoDB（MongoDB Atlas）
- Render（Dockerでデプロイ）

## データ設計（RDB/SQLで表すと）

本アプリは MongoDB（ドキュメントDB）で実装していますが、RDB（PostgreSQL等）で表すと以下の関係になります。

### ER（関係）

- `users`（ユーザー）
  - 1人のユーザーは複数の応募を持つ（1:N）
- `job_applications`（応募）
  - 1つの応募は複数の履歴を持つ（1:N）
- `job_histories`（応募履歴）

```
users (1) ──< (N) job_applications (1) ──< (N) job_histories
```

### PostgreSQL想定DDL（例）

```sql
-- users
create table users (
  id            bigserial primary key,
  username      varchar(64) not null unique,
  password_hash text        not null,
  created_at    timestamptz not null default now()
);

-- job applications
create table job_applications (
  id              bigserial primary key,
  owner_user_id   bigint      not null references users(id) on delete cascade,
  company_name    text        not null,
  status          text        not null,
  interview_date  date,
  memo            text,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now()
);

create index idx_job_applications_owner_interview_date
  on job_applications (owner_user_id, interview_date);

create index idx_job_applications_owner_status
  on job_applications (owner_user_id, status);

-- job histories
create table job_histories (
  id                 bigserial primary key,
  job_application_id bigint      not null references job_applications(id) on delete cascade,
  event_date         date,
  action             text        not null,
  note               text,
  created_at         timestamptz not null default now()
);

create index idx_job_histories_application_event_date
  on job_histories (job_application_id, event_date);
```

補足:

- 実装（MongoDB）では `job_histories` は別テーブルではなく、`job_applications` の配列として埋め込み（サブドキュメント）で保持しています。
- 認可（Authorization）として、応募データは常に「ログイン中ユーザーの所有データ」にスコープする前提（RDBなら `owner_user_id`、Mongoなら `ownerUserId`）です。

## 環境変数（MongoDB）

MongoDB接続URIを以下のどちらかで設定してください（どちらか1つでOK）。

- `SPRING_DATA_MONGODB_URI`（推奨）
- `MONGODB_URI`

※ URIの中にユーザー名/パスワード等の機密情報が含まれるため、READMEやGitHubに値は載せないでください。

### Render（本番）

Renderの Web Service → `Environment` で上記KEYを追加して設定します。

### ローカル（例）

ターミナルで環境変数を設定して起動します（値は自分のものに置き換え）。

```bash
export SPRING_DATA_MONGODB_URI="YOUR_MONGODB_URI"
./mvnw spring-boot:run
```

## ローカル起動

前提:

- Java 17

起動:

```bash
./mvnw spring-boot:run
```

アクセス:

- `http://localhost:8081/applications`

## テスト

```bash
./mvnw test
```

## 実装上の工夫

- 削除はGETで副作用を起こさないようにし、POSTでのみ削除する設計に変更（リンクを踏んだだけで削除される事故を防止）
- 旧URL（`/applications/new`）にアクセスされても新規登録画面へ遷移するよう互換ルートを用意

