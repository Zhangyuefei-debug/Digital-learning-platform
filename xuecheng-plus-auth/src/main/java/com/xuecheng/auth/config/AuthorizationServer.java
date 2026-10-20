package com.xuecheng.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;

import javax.annotation.Resource;

/**
 * @description 授权服务器配置
 * @author Mr.M
 * @date 2022/9/26 22:25
 * @version 1.0
 */
 @Configuration
 @EnableAuthorizationServer
 public class AuthorizationServer extends AuthorizationServerConfigurerAdapter {

  @Resource(name="authorizationServerTokenServicesCustom")
  private AuthorizationServerTokenServices authorizationServerTokenServices;

 @Autowired
 private AuthenticationManager authenticationManager;

  //客户端详情服务
  @Override
  public void configure(ClientDetailsServiceConfigurer clients)
          throws Exception {
        clients.inMemory()// 使用in-memory存储
                .withClient("XcWebApp")// client_id
                //.secret("XcWebApp")//客户端密钥
                .secret(new BCryptPasswordEncoder().encode("XcWebApp"))//客户端密钥
                .resourceIds("xuecheng-plus")//资源列表
                .authorizedGrantTypes("authorization_code", "password","client_credentials","implicit","refresh_token")// 该client允许的授权类型authorization_code,password,refresh_token,implicit,client_credentials
                .scopes("all")// 允许的授权范围
                .autoApprove(false)//false跳转到授权页面
                //客户端接收授权码的重定向地址
                .redirectUris("http://www.51xuecheng.cn")
   ;
  }


  //令牌端点的访问配置
  @Override
  public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
   endpoints
           .authenticationManager(authenticationManager)//认证管理器
           .tokenServices(authorizationServerTokenServices)//令牌管理服务
           .allowedTokenEndpointRequestMethods(HttpMethod.POST);
  }

  //令牌端点的安全配置
  @Override
  public void configure(AuthorizationServerSecurityConfigurer security){
   security
           .tokenKeyAccess("permitAll()")                    //oauth/token_key是公开
           .checkTokenAccess("permitAll()")                  //oauth/check_token公开
           .allowFormAuthenticationForClients()				//表单认证（申请令牌）
   ;
  }
 }

 //这个文件定义了如何颁发数字护照（JWT），以及哪些客户有资格来申请。
//概念,代码中的对应项,解释
//护照申请人（客户端）,"clients.inMemory().withClient(""XcWebApp"")",你定义了谁有权代表用户来申请护照。这里是你的 Web 应用（XcWebApp）。它有自己的 ID 和密钥。
//授权模式,"authorizedGrantTypes(""password"", ""refresh_token"", ...)",你允许客户端使用哪些方式来获取护照。最常用的是 password（用户输入密码登录）和 refresh_token（护照快过期时，用旧护照换新护照）。
//颁发流程,endpoints.tokenServices(authorizationServerTokenServices),你指定了用 authorizationServerTokenServices（即下一步的 JWT 服务）来生成和管理护照（JWT）。
//总结： AuthorizationServer 负责 OAuth 2.0 协议流程。它是一个框架，用来管理客户端如何通过各种授权方式来获取令牌。