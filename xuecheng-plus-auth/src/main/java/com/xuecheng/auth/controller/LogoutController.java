package com.xuecheng.auth.controller;

import com.xuecheng.auth.config.JwtBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class LogoutController {

    @Autowired
    private TokenStore tokenStore;
    @Autowired
    private JwtBlacklistService blacklistService;

    @PostMapping("/logout")
    public Mono<String> logout(ServerWebExchange exchange) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization").split(" ")[1];
        if (token != null) {
            OAuth2AccessToken accessToken = tokenStore.readAccessToken(token);
            if (accessToken != null) {
                long expiresIn = accessToken.getExpiresIn();
                blacklistService.addTokenToBlacklist(token, expiresIn);
            }
        }
        return Mono.just("登出成功");
    }

}
