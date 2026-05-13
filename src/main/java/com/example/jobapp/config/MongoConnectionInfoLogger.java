package com.example.jobapp.config;

import java.net.URI;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.mongodb.ConnectionString;

/**
 * Render / Atlas の「どこに接続しているか」切り分け用。
 * 機密情報（ユーザー名/パスワード）は出さず、ホスト名とDB名のみログ出力する。
 */
@Component
public class MongoConnectionInfoLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoConnectionInfoLogger.class);

    @Value("${spring.data.mongodb.uri:}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:}")
    private String mongoDatabase;

    private static String safeUriDiagnostic(String uri) {
        if (uri == null || uri.isBlank()) return "empty";

        // パスワード等は出さない。できる範囲で scheme と host/clusterを示す
        try {
            ConnectionString cs = new ConnectionString(uri);
            boolean srv = uri.startsWith("mongodb+srv://");
            String hosts = cs.getHosts() == null ? "" : cs.getHosts().stream().collect(Collectors.joining(","));
            boolean looksLocalhost = hosts.contains("127.0.0.1") || hosts.contains("localhost");
            return String.format("{scheme=mongodb%s,hosts=[%s],looksLocal=%s}",
                    srv ? "+srv" : "",
                    hosts,
                    looksLocalhost);
        } catch (Exception ignored) {
            boolean looksMalformed = !(uri.startsWith("mongodb://") || uri.startsWith("mongodb+srv://"));
            return "{unparseable=" + looksMalformed + "}";
        }
    }

    private static String safeHostSummary(String uri) {
        if (uri == null || uri.isBlank()) return "";

        // mongodb+srv は java.net.URI で host が取れないことがあるため ConnectionString を優先
        try {
            ConnectionString cs = new ConnectionString(uri);
            if (cs.getHosts() != null && !cs.getHosts().isEmpty()) {
                return cs.getHosts().stream().collect(Collectors.joining(","));
            }
        } catch (Exception ignored) {
            // fallthrough
        }

        try {
            URI u = URI.create(uri);
            return u.getHost() == null ? "" : u.getHost();
        } catch (Exception ignored) {
            return "";
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        // Render 側の設定値がアプリ内で見えているか（秘密情報は出さない）
        String envMongo = System.getenv("MONGODB_URI");
        String envSpringMongo = System.getenv("SPRING_DATA_MONGODB_URI");
        log.info("MongoDB env presence: MONGODB_URI={}, SPRING_DATA_MONGODB_URI={}",
                (envMongo != null && !envMongo.isBlank()),
                (envSpringMongo != null && !envSpringMongo.isBlank()));
        log.info("MongoDB env host summary: MONGODB_URI=[{}], SPRING_DATA_MONGODB_URI=[{}]",
                safeHostSummary(envMongo),
                safeHostSummary(envSpringMongo));
        log.info("MongoDB spring binding (sanitized): spring.data.mongodb.uri={}", safeUriDiagnostic(mongoUri));

        if (mongoUri == null || mongoUri.isBlank()) {
            log.warn("MongoDB: spring.data.mongodb.uri is empty. (Embedded Mongo may be used depending on runtime)");
            return;
        }

        try {
            ConnectionString cs = new ConnectionString(mongoUri);
            String hosts = cs.getHosts() == null ? "" : cs.getHosts().stream().collect(Collectors.joining(","));
            String dbFromUri = cs.getDatabase();
            log.info("MongoDB connection target: hosts=[{}], database(property)=[{}], database(uri)=[{}]",
                    hosts,
                    (mongoDatabase == null ? "" : mongoDatabase),
                    (dbFromUri == null ? "" : dbFromUri));
        } catch (Exception e) {
            log.warn("MongoDB: failed to parse spring.data.mongodb.uri for diagnostics (will not log uri).", e);
        }
    }
}

