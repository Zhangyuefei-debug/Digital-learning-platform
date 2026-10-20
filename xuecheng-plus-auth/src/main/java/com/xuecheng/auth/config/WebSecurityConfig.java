package com.xuecheng.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;


@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true,prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {


    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
//        //密码为明文方式
        //return NoOpPasswordEncoder.getInstance();
        return new BCryptPasswordEncoder();
    }


    @Autowired
    DaoAuthenticationProviderCustom daoAuthenticationProviderCustom;


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(daoAuthenticationProviderCustom);
    }

    //配置安全拦截机制
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/r/**").authenticated()//访问/r开始的请求需要认证通过
                .anyRequest().permitAll()//其它请求全部放行
                .and()
                .formLogin().successForwardUrl("/login-success");//登录成功跳转到/login-success
    }
}
//这个文件定义的是用户如何向“护照颁发中心”证明**“我是我”，即用户认证**。
//概念,代码中的对应项,解释
//登录验证机制,configure(AuthenticationManagerBuilder),你告诉系统：当用户提交用户名和密码时，去使用 DaoAuthenticationProviderCustom（你的自定义用户加载逻辑）来验证他们是否是合法用户。
//密码安全,BCryptPasswordEncoder,你确保用户密码在数据库中是安全的（加密的），并且只有使用这个加密器，才能比对用户输入的明文密码是否正确。
//自身保护,configure(HttpSecurity),你定义了护照颁发中心自己的网站安全规则。例如，只有 /r/** 的路径需要登录（可能是一些管理端点），其他路径（如登录页）允许所有人访问。
//总结： WebSecurityConfig 负责用户的身份确认。只有通过了这里的验证，用户才能进入下一步，申请数字护照（JWT）。
