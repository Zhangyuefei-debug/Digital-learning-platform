package com.xuecheng.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import java.util.Arrays;

/**
 * @author Administrator
 * @version 1.0
 **/
@Configuration
public class TokenConfig {             //这段代码是 认证服务（Authorization Server） 的核心配置，它的作用就是 配置并启用 JWT 令牌的生成和管理机制。


    private String SIGNING_KEY = "mq123";

    @Autowired
    TokenStore tokenStore;



    //作用： 启用 InMemoryTokenStore。
    //特点： 令牌（Token）被直接存储在授权服务器的内存中。
    //优点： 配置简单，常用于开发或测试。
    //缺点： 不适用于生产环境。一旦授权服务器重启或宕机，所有已颁发的令牌都会丢失；资源服务器需要频繁地回调授权服务器来验证令牌的有效性（称为自省/内省），这会增加系统开销。
//    @Bean
//    public TokenStore tokenStore() {
//        //使用内存存储令牌（普通令牌）
//        return new InMemoryTokenStore();
//    }

    @Autowired
    private JwtAccessTokenConverter accessTokenConverter;


    //作用： 启用 JwtTokenStore，它依赖于 JwtAccessTokenConverter。
    //特点： 令牌本身是一个自包含的 JSON Web Token (JWT)。它包含了用户、权限、过期时间等信息，并使用SIGNING_KEY进行数字签名。
    //优点：
    //无状态性 (Stateless)： 令牌信息存储在令牌本身，授权服务器无需存储这些令牌信息。
    //高性能： 资源服务器（Resource Server）可以通过 JWT 中的公钥或共享密钥在本地直接验证令牌的有效性和完整性，无需每次都去请求授权服务器，极大地提高了微服务架构中的性能和可扩展性。
    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());  //告诉 Spring Security 使用 JWT 作为存储方式（无状态，不保存到数据库或内存）。你不用自己解析 token，Spring Security 会帮你。
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() { //负责 签名和解析 JWT。
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey(SIGNING_KEY);
        return converter;
    }

    //令牌管理服务
    @Bean(name="authorizationServerTokenServicesCustom")           //这是 Spring Security 的“管理 JWT 的服务”，包括：token生成   刷新token   设置过期时间 你不用每次手动生成 token，框架帮你做了。
    public AuthorizationServerTokenServices tokenService() {
        DefaultTokenServices service=new DefaultTokenServices();
        service.setSupportRefreshToken(true);//支持刷新令牌
        service.setTokenStore(tokenStore);//令牌存储策略

        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(accessTokenConverter));
        service.setTokenEnhancer(tokenEnhancerChain);

        service.setAccessTokenValiditySeconds(7200); // 令牌默认有效期2小时
        service.setRefreshTokenValiditySeconds(259200); // 刷新令牌默认有效期3天
        return service;
    }
}
//这个文件定义了护照（JWT）本身的结构、安全机制和有效期。
//概念,代码中的对应项,解释
//防伪印章（密钥）,"SIGNING_KEY = ""mq123""",这是整个系统的共享密钥。它就像是护照颁发中心盖在护照上的防伪钢印。只有拥有这个密钥的机构，才能验证这个护照是不是真的、有没有被篡改。
//护照格式（JWT）,JwtAccessTokenConverter,这个组件负责把用户的身份信息（ID、权限等）打包成一个自包含的、带防伪钢印的字符串（JWT）。
//存储策略,tokenStore() -> new JwtTokenStore(...),你选择了一种“无状态”的存储方式。这意味着：护照颁发中心不留底！ 它把护照给了用户，就不再管了。未来其他服务验证护照，不需要再问颁发中心，自己用“防伪钢印密钥”验一下就行。 这是微服务高性能的关键。
//总结： TokenConfig 负责 JWT 的生成和安全验证。


//🔒 用户登录与令牌颁发（JWT）的 5 个步骤
//步骤 1: 用户提交请求（WebSecurityConfig 的前置）
//用户行为： 用户在登录页面输入用户名、密码（可能还有验证码），点击登录。
//
//前端动作： 前端应用（XcWebApp 客户端）将这些信息打包，向授权服务器（Auth Server）发起一个 HTTP POST 请求，目标是 /oauth/token 端点。这个请求还会带上客户端自身的身份信息（client_id 和 client_secret）。
//
//授权服务接收： 请求首先被 Auth Server 的 Spring Security 过滤器链捕获。
//
//步骤 2: 用户身份认证（WebSecurityConfig 的核心）
//认证管理器： 请求到达你的 AuthorizationServer 配置中定义的 authenticationManager。
//
//数据库比对： authenticationManager 会调用你在 WebSecurityConfig 中配置的 DaoAuthenticationProviderCustom。
//
//你的自定义 Provider 会根据用户名去数据库查询用户信息。
//
//它使用 BCryptPasswordEncoder（你在 WebSecurityConfig 中配置的）来校验用户输入的密码是否与数据库中加密后的密码匹配。
//
//验证结果：
//
//如果验证失败： 返回 401 错误，流程结束。
//
//如果验证成功： Spring Security 成功构建了一个代表该用户的身份对象，证明 “用户是合法的”。
//
//步骤 3: 令牌颁发与签名（AuthorizationServer 和 TokenConfig 的核心）
//用户身份被确认合法后，授权服务器开始颁发“数字护照”（JWT）：
//
//令牌生成： AuthorizationServer 调用你在 TokenConfig 中配置的 AuthorizationServerTokenServices。
//
//JWT 编码： TokenConfig 中的 JwtAccessTokenConverter 开始工作：
//
//它将用户的 ID、权限列表、过期时间等信息，编码（Payload）进 JWT 结构。
//
//它使用你在 TokenConfig 中定义的 SIGNING_KEY = "mq123" 对这个 JWT 进行数字签名（Signature）。
//
//令牌返回： Auth Server 将完整的 JWT 字符串（通常在 access_token 字段）返回给前端应用。
//
//步骤 4: JWT 的存储（客户端行为）
//JWT 存储在哪里？
//
//服务器端： 不存储。因为你在 TokenConfig 中使用了 JwtTokenStore，这是一种无状态的存储策略，授权服务器颁发后就“不留底”了。
//
//客户端 (Client)： 客户端（浏览器或 App）接收到 JWT 后，通常将其存储在以下位置之一：
//
//Cookie： （如果配置了 HttpOnly 属性会更安全，你的 GatewayAuthFilter 代码似乎可以从 Cookie 中读取）
//
//本地存储 (localStorage) 或会话存储 (sessionStorage)
//
//步骤 5: 网关的守卫作用（后续资源请求）
//用户登录成功并拿到 JWT 之后，所有对业务微服务（如内容管理服务）的访问，都必须通过网关。
//
//客户端附带令牌： 客户端在访问内容管理服务的任何 URL 时，都会将存储的 JWT 放到 Authorization 请求头中（格式为 Authorization: Bearer <JWT>）。
//
//网关发挥作用： 请求到达 GatewayAuthFilter。
//
//网关校验： 你的 GatewayAuthFilter 调用 tokenStore.readAccessToken(token)。由于网关也知道 SIGNING_KEY，它会在本地快速完成以下检查：
//
//验签： 确认 JWT 签名是否有效（检查令牌是否被篡改）。
//
//过期： 确认令牌是否过期。
//
//结果：
//
//失败： 网关返回 401 错误，请求被拦截。
//
//成功： 网关放行，将请求路由到内容管理服务（并将 JWT 包含在请求头中转发下去）。