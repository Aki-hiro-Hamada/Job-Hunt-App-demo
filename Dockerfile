# ビルドステージ（公式 Maven イメージ）
# Render 等で Maven がメモリ不足で落ちる場合があるためヒープを抑える
FROM maven:3.9.6-eclipse-temurin-17 AS build
ENV MAVEN_OPTS="-Xmx512m -XX:+UseSerialGC -Djava.awt.headless=true"
WORKDIR /app
COPY . .
RUN mvn -B -DskipTests clean package

# 実行ステージ
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# 実行可能 JAR は repackage 後の 1 本（pom の version と一致）
COPY --from=build /app/target/job-hunting-app-0.0.1-SNAPSHOT.jar app.jar

# ローカル既定は 8081。Render は環境変数 PORT を参照（application.properties）
EXPOSE 8081

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

# server.port は application.properties の ${PORT:8081} が参照（JSON形式の ENTRYPOINT では ${} が展開されないため CLI には書かない）
ENTRYPOINT ["java", "-jar", "app.jar"]
