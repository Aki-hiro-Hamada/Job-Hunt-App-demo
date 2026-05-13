package com.example.jobapp.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Render 等で {@code MONGODB_URI} が空文字のまま残っていると、
 * {@code spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI:${MONGODB_URI:...}}} が空に解決され、
 * Mongo ドライバが「接続文字列が無効」と起動失敗する。空は未設定と同様に扱い、
 * 先に中身のある変数へフォールバックする。
 */
public class MongoConnectionUriEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SPRING_URI_KEY = "SPRING_DATA_MONGODB_URI";
    private static final String LEGACY_URI_KEY = "MONGODB_URI";
    private static final String TARGET_PROP = "spring.data.mongodb.uri";
    private static final String DEFAULT_LOCAL = "mongodb://localhost:27017";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String spring = environment.getProperty(SPRING_URI_KEY);
        String legacy = environment.getProperty(LEGACY_URI_KEY);
        String chosen = firstNonBlank(spring, legacy);
        if (!StringUtils.hasText(chosen)) {
            chosen = DEFAULT_LOCAL;
        } else {
            chosen = chosen.trim();
        }
        Map<String, Object> map = new HashMap<>();
        map.put(TARGET_PROP, chosen);
        environment.getPropertySources().addFirst(new MapPropertySource("resolvedMongoConnectionUri", map));
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a.trim();
        }
        if (StringUtils.hasText(b)) {
            return b.trim();
        }
        return null;
    }
}
