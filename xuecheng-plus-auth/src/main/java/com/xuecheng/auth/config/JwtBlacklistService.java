package com.xuecheng.auth.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class JwtBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void addTokenToBlacklist(String token, long expirationSeconds) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "invalid", expirationSeconds, TimeUnit.SECONDS);
    }
}
