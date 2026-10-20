package com.xuecheng.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * @description 安全配置类
 * @author Mr.M
 * @date 2022/9/27 12:07
 * @version 1.0
 */
 @EnableWebFluxSecurity
 @Configuration
 public class SecurityConfig {


  //安全拦截配置
  @Bean
  public SecurityWebFilterChain webFluxSecurityFilterChain(ServerHttpSecurity http) {

   return http.authorizeExchange()
           .pathMatchers("/**").permitAll()
           .anyExchange().authenticated()
           .and().csrf().disable().build();
  }

  //上述这段代码总结： 这段代码为 Spring Cloud Gateway 本身定义了一个 WebFlux 安全链。然而，由于配置了 .pathMatchers("/**").permitAll()，这个 WebFlux 安全链实际上是 放行了所有请求，它并不是主要的认证校验点。主要的认证逻辑在下一个 GatewayAuthFilter 中实现。
 }
