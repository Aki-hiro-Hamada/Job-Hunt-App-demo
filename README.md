# Job-Hunt-App-demo（内定ホイホイ・デモ版）

応募先（企業）を登録し、選考ステータス・日付・メモ・履歴を管理するWebアプリの**デモ用フォーク**です。本番版（`Job-Hunt-App`）と同じ機能ながら、データベースを **Supabase (PostgreSQL)** に置き換えています。

- 公開URL（採用共有用）: `https://job-hunt-app-demo.onrender.com`

## 本番版との違い

| 項目 | 本番（Job-Hunt-App） | デモ（Job-Hunt-App-demo） |
|---|---|---|
| データベース | MongoDB Atlas | **Supabase (PostgreSQL)** |
| データアクセス | Spring Data MongoDB | Spring Data JPA / Hibernate |
| ID 型 | `String`（ObjectId） | `Long`（BIGSERIAL） |
| 履歴の保持 | サブドキュメント | 別テーブル + `@OneToMany` |

## 制作の目的

- 採用担当者への共有時は個人の本番データと切り離すため、デモ用の公開URLとデータベースを別途用意する運用とするため。
- 同じドメインロジックを **MongoDB / PostgreSQL** で書き分けることで、データモデリングの違いを学習・検証する。

## 主な機能（本番と同じ）

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
- Spring Data JPA / Hibernate
- Thymeleaf
- PostgreSQL（**Supabase**）
- Render（Docker でデプロイ）

## データモデル（PostgreSQL）

```
users (1) ──< (N) job_applications (1) ──< (N) job_histories
```

`@Entity` クラスから Hibernate が自動生成する DDL（参考）:

```sql
create table users (
  id            bigserial primary key,
  username      varchar(255) not null unique,
  password_hash varchar(255) not null,
  created_at    timestamp(6) with time zone not null
);

create table job_applications (
  id             bigserial primary key,
  owner_user_id  varchar(255) not null,
  company_name   varchar(255) not null,
  status         varchar(255) not null,
  interview_date date,
  memo           text
);

create table job_histories (
  id                  bigserial primary key,
  job_application_id  bigint references job_applications(id),
  event_date          date,
  action              varchar(255),
  note                text
);
```

補足:

- 認可（Authorization）として、応募データは常に「ログイン中ユーザーの所有データ」にスコープします（`owner_user_id` にログインユーザー名を保持）。
- 起動時に `spring.jpa.hibernate.ddl-auto=update` でテーブルが自動作成・更新されます（Supabase 側で手作業のテーブル作成は不要）。

## 環境変数（Supabase）

| KEY | 例 / 用途 |
|---|---|
| `SUPABASE_DB_PASSWORD` | **必須**。Supabase の Database Password |
| `SPRING_DATASOURCE_URL` | 任意。既定は `application.properties` の Supabase Session pooler |
| `SPRING_DATASOURCE_USERNAME` | 任意。既定は `postgres.＜project-ref＞` |

接続文字列は **Session pooler**（`pooler.supabase.com:5432`）を使う前提です。`db.＜project-ref＞.supabase.co`（Direct connection）は IPv6 のみのため、Render Free からは接続できません。

### Render（本番デプロイ）

Render の Web Service → `Environment` で少なくとも `SUPABASE_DB_PASSWORD` を設定します（URL/USERNAME はソース側にデフォルト値あり）。

### ローカル

```bash
export SUPABASE_DB_PASSWORD='＜Supabaseのパスワード＞'
./mvnw spring-boot:run
# または ./run-local.sh
```

ローカルでオフライン用に Docker の PostgreSQL を使いたい時は:

```bash
./run-local.sh --local-pg
```

## ローカル起動

前提:

- Java 17
- Supabase プロジェクト + Database Password（または Docker / Docker Compose）

起動:

```bash
export SUPABASE_DB_PASSWORD='...'
./mvnw spring-boot:run
```

アクセス: `http://localhost:8081/applications`

## テスト

```bash
./mvnw test
```

## 実装上の工夫

- 削除は GET で副作用を起こさないようにし、POST でのみ削除する設計（リンクを踏んだだけで削除される事故を防止）
- 旧URL（`/applications/new`）にアクセスされても新規登録画面へ遷移するよう互換ルートを用意
- 編集時に `jobHistories` が POST に含まれないことを利用して既存履歴が消える事故を、Service 側で既存レコードを読み直してから値だけを反映することで防止
