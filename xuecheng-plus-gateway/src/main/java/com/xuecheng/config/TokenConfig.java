package com.xuecheng.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

/**
 * @author Administrator
 * @version 1.0
 **/
@Configuration
public class TokenConfig {

    String SIGNING_KEY = "mq123";


//    @Bean
//    public TokenStore tokenStore() {
//        //使用内存存储令牌（普通令牌）
//        return new InMemoryTokenStore();
//    }

    @Autowired
    private JwtAccessTokenConverter accessTokenConverter;

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey(SIGNING_KEY);
        return converter;
    }

    //上述这段代码它提供了 JWT 令牌校验的核心工具 (TokenStore 和 JwtAccessTokenConverter)，允许网关在不访问授权服务器的情况下，本地判断令牌的合法性。

}


//                ┌────────────────────────────┐
//                │        用户登录请求        │
//                │ POST /auth/oauth/token     │
//                └────────────┬───────────────┘
//                             │
//                             ▼
//                     [认证服务 AuthorizationServer]
//                             │
//                             │ TokenConfig → JwtAccessTokenConverter(签名: mq123)
//                             │
//                             ▼
//                       生成 JWT 令牌返回
//                             │
//                             ▼
//          ┌────────────────────────────────────────────┐
//          │                浏览器 / 前端App             │
//          │        (携带JWT令牌访问受保护接口)          │
//          └────────────────────────────────────────────┘
//                               │
//                               ▼
//           ┌──────────────────────────────┐
//           │   Spring Cloud Gateway 网关   │
//           │                              │
//           │ 1️⃣ SecurityConfig            │
//           │    └─ 放行所有请求（permitAll） │
//           │       不做拦截，只负责让请求进入过滤链   │
//           │                              │
//           │ 2️⃣ GatewayAuthFilter         │
//           │    ├─ 从请求头读取JWT Token         │
//           │    ├─ 解析出用户信息（userId、权限等） │
//           │    ├─ 校验Token是否有效（签名/过期）  │
//           │    └─ 若有效则放行，否则返回401        │
//           └──────────────────────────────┘
//                               │
//                               ▼
//          ┌───────────────────────────────────────┐
//          │     下游微服务（content-service等）     │
//          │                                       │
//          │ 3️⃣ 微服务自己的 Spring Security 配置     │
//          │    ├─ 基于JWT中的用户信息构造SecurityContext │
//          │    ├─ 根据权限控制访问具体接口 (@PreAuthorize等) │
//          │    └─ 返回资源数据给客户端                  │
//          └───────────────────────────────────────┘
//                               │
//                               ▼
//          ┌────────────────────────────────────────────┐
//          │                返回数据给前端                │
//          └────────────────────────────────────────────┘