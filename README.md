# 内定ホイホイ

就職活動の応募先・選考状況・面接メモをまとめて管理する Web アプリです。職業訓練で Java と SQL を学びながら作成したポートフォリオ作品です。

**公開URL:** https://job-hunt-app-demo.onrender.com  
（初回アクセス時、起動まで 30 秒ほどかかることがあります）

## 使っている技術

- Java 17 / Spring Boot
- PostgreSQL（Supabase）
- HTML / Thymeleaf
- Render（公開）

## できること

- 応募先の登録・編集・削除・一覧表示
- 選考ステータス（応募前〜内定・お見送りなど）の管理
- 面接日や内容のメモ（1 社につき複数件）
- ログインした人だけが自分のデータを見られる

## データの関係（イメージ）

```
users ──< job_applications ──< job_histories
（ユーザー 1 人に応募先が複数、応募先 1 件に履歴が複数）
```

## ローカルで動かす（任意）

Java 17 と Docker があれば、次の 1 コマンドで起動できます。

```bash
./run-local.sh --local-pg
```

ブラウザで http://localhost:8081/applications を開きます（初回は `/register` でユーザー登録）。

テスト: `./mvnw test`
