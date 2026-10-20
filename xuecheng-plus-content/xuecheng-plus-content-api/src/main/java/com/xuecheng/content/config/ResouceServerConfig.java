package com.xuecheng.content.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;

import javax.servlet.http.HttpServletResponse;


@Configuration
@EnableResourceServer //启用 Spring Security OAuth2 的资源服务器功能。这是开启所有相关功能的开关。
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
//启用方法级别的安全控制。允许您在 Service 或 Controller 的方法上使用 @PreAuthorize 或 @Secured 等注解，根据 JWT 中的权限信息来限制方法的访问。
public class ResouceServerConfig extends ResourceServerConfigurerAdapter {   //该配置类的作用是实现用户授权访问。


    //资源服务标识
    public static final String RESOURCE_ID = "xuecheng-plus";   //设置当前资源服务器的唯一标识符（xuecheng-plus）。授权服务器会检查 JWT 中的 aud (audience) 字段是否包含此 ID。

    @Autowired
    TokenStore tokenStore;

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {            //注入 JWT 验证机制。 将TokenConfig 中配置好的 JwtTokenStore 注入到资源服务器配置中，告诉它使用 JWT 进行身份验证。
        resources.resourceId(RESOURCE_ID)//资源 id
                .tokenStore(tokenStore)
                .stateless(true);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                //.antMatchers("/course/**").authenticated()//所有/course/**的请求必须认证通过
                .anyRequest().permitAll();
    }
}

