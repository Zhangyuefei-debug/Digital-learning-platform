package com.xuecheng.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class JwtBlacklistService  {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Autowired
    private StringRedisTemplate redisTemplate;


    // 判断 token 是否在黑名单
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }


}
