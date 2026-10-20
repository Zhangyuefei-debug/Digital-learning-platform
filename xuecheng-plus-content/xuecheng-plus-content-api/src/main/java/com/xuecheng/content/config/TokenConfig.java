package com.xuecheng.content.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import java.util.Arrays;

/**
 * @author Administrator
 * @version 1.0
 **/
@Configuration
public class TokenConfig {  //该配置类的作用是实现用户认证

    String SIGNING_KEY = "mq123";   //定义了用于签名和验证 JWT 的共享密钥。它必须与授权服务器生成 JWT 时使用的密钥一致。


//    @Bean
//    public TokenStore tokenStore() {
//        //使用内存存储令牌（普通令牌）
//        return new InMemoryTokenStore();
//    }

    @Autowired
    private JwtAccessTokenConverter accessTokenConverter;   //核心转换器。它知道如何用 SIGNING_KEY 对 JWT 进行解码、验签（验证签名）以及提取 Claims（用户信息和权限）。

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());   //告诉 Spring Security 令牌是 JWT 格式。当收到请求时，资源服务器会使用 JwtTokenStore 结合 JwtAccessTokenConverter 在本地完成令牌的全部校验工作。
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey(SIGNING_KEY);
        return converter;
    }
}

//可以拆开来看：
//
//JwtAccessTokenConverter
//
//负责 JWT 编码和解码。
//
//你在这里设置了 SIGNING_KEY（mq123），这意味着内容管理服务可以用同一个密钥去验证 JWT 的签名是否正确。
//
//它还能提取 JWT 里的用户信息（claims），比如 userId、roles 等。
//
//JwtTokenStore
//
//告诉 Spring Security 令牌是 JWT 格式，不需要去数据库查。
//
//当请求携带 JWT 时，Spring Security 会通过这个 TokenStore + JwtAccessTokenConverter 在本地完成 token 校验。