package com.example.excelimport.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private static final String KEY_PREFIX = "auth:token:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final long ttlSeconds;
    private final Map<String, LocalToken> localTokenStore = new ConcurrentHashMap<>();

    public TokenService(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                        @Value("${app.auth.token-ttl-seconds:43200}") long ttlSeconds) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(String username) {
        byte[] raw = new byte[32];
        SECURE_RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(KEY_PREFIX + token, username, Duration.ofSeconds(ttlSeconds));
        } else {
            localTokenStore.put(token, new LocalToken(username, Instant.now().plusSeconds(ttlSeconds)));
        }
        return token;
    }

    public String resolveUsername(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        if (redisTemplate != null) {
            return redisTemplate.opsForValue().get(KEY_PREFIX + token);
        }
        LocalToken localToken = localTokenStore.get(token);
        if (localToken == null) {
            return null;
        }
        if (Instant.now().isAfter(localToken.expiresAt())) {
            localTokenStore.remove(token);
            return null;
        }
        return localToken.username();
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        if (redisTemplate != null) {
            redisTemplate.delete(KEY_PREFIX + token);
        } else {
            localTokenStore.remove(token);
        }
    }

    private record LocalToken(String username, Instant expiresAt) {
    }
}
