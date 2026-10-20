package com.xuecheng.config;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;


@Component
@Slf4j
public class GatewayAuthFilter implements GlobalFilter, Ordered {              //功能：实现认证和放行逻辑


    //白名单
    private static List<String> whitelist = null;

    static {
        //加载白名单  从 /security-whitelist.properties 文件中加载不需要认证的 URL 路径，例如登录、注册等。
        try (
                InputStream resourceAsStream = GatewayAuthFilter.class.getResourceAsStream("/security-whitelist.properties")
        ) {
            Properties properties = new Properties();
            properties.load(resourceAsStream);
            Set<String> strings = properties.stringPropertyNames();
            whitelist= new ArrayList<>(strings);

        } catch (Exception e) {
            log.error("加载/security-whitelist.properties出错:{}",e.getMessage());
            e.printStackTrace();
        }
    }

    @Autowired
    private TokenStore tokenStore;
    @Autowired
    private JwtBlacklistService blacklistService;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestUrl = exchange.getRequest().getPath().value();
        AntPathMatcher pathMatcher = new AntPathMatcher();
        //白名单放行
        for (String url : whitelist) {
            if (pathMatcher.match(url, requestUrl)) {
                return chain.filter(exchange);
            }
        }

        //检查token是否存在
        String token = getToken(exchange);
        if (StringUtils.isBlank(token)) {
            return buildReturnMono("没有认证",exchange);
        }
        //判断是否是有效的token
        OAuth2AccessToken oAuth2AccessToken;
        try {
            oAuth2AccessToken = tokenStore.readAccessToken(token);      //调用 tokenStore.readAccessToken(token) 进行本地验签和解析，并检查 isExpired() 状态。 //总结
                                                                        //您不需要在 GatewayAuthFilter 中看到密钥，因为：
                                                                        //密钥和解密逻辑被封装在了 JwtAccessTokenConverter 对象中。
                                                                        //TokenConfig 将 JwtAccessTokenConverter 注入并封装进了 JwtTokenStore。
                                                                        //GatewayAuthFilter 通过 依赖注入 获得了配置好的 TokenStore。
            boolean expired = oAuth2AccessToken.isExpired();
            if (expired) {
                return buildReturnMono("认证令牌已过期",exchange);
            }
            // ✅ 强制失效检查
            if (blacklistService.isTokenBlacklisted(token)) {
                return buildReturnMono("用户已被强制下线", exchange);
            }
            return chain.filter(exchange);
        } catch (InvalidTokenException e) {
            log.info("认证令牌无效: {}", token);
            return buildReturnMono("认证令牌无效",exchange);    //封装统一的错误响应。如果令牌缺失、过期或无效，则返回 401 Unauthorized 状态码。
        }
    }

    /**
     * 获取token
     */
//    private String getToken(ServerWebExchange exchange) {       //获取 JWT 令牌。它从 HTTP 请求的 Authorization 头部（格式为 Bearer <Token>）中提取出 JWT 字符串。
//        String tokenStr = exchange.getRequest().getHeaders().getFirst("Authorization");
//        if (StringUtils.isBlank(tokenStr)) {
//            return null;
//        }
//        String token = tokenStr.split(" ")[1];
//        if (StringUtils.isBlank(token)) {
//            return null;
//        }
//        return token;
//    }

    private String getToken(ServerWebExchange exchange) {
        // 1️⃣ 先从 Authorization 头读取
        String tokenStr = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (!StringUtils.isBlank(tokenStr) && tokenStr.startsWith("Bearer ")) {
            return tokenStr.split(" ")[1];
        }

        // 2️⃣ 如果 Header 没有，再从 Cookie 读取
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst("jwt");
        if (cookie != null) {
            return cookie.getValue();
        }

        // 3️⃣ 如果都没有，返回 null
        return null;
    }





    private Mono<Void> buildReturnMono(String error, ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        String jsonString = JSON.toJSONString(new RestErrorResponse(error));
        byte[] bits = jsonString.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bits);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        return response.writeWith(Mono.just(buffer));
    }


    @Override
    public int getOrder() {
        return 0;
    }
}
