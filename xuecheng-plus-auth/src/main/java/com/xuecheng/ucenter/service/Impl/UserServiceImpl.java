package com.xuecheng.ucenter.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class UserServiceImpl implements UserDetailsService {  //用户提交账号和密码由DaoAuthenticationProvider调用UserDetailsService的loadUserByUsername()方法获取UserDetails用户信息。

    @Autowired
    private XcUserMapper xcUserMapper;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    private XcMenuMapper menuMapper;
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        AuthParamsDto authParamsDto = null;  //统一认证入口所需的实体类
        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);  //首先将前端传入的json串转为AuthParamsDto对象
        } catch (Exception e) {
            log.error("解析用户信息失败:{}",e.getMessage());
            throw new RuntimeException("解析用户信息失败");
        }

        //认证类型
        String authType=authParamsDto.getAuthType();
        //根据认证类型从spring 容器中获取指定的bean
        AuthService authService = applicationContext.getBean(authType + "_authservice", AuthService.class);

        //开始认证 认证所需要的用户信息存储在用户中心数据库，现在需要将认证服务连接数据库查询用户信息
        XcUserExt user = authService.execute(authParamsDto);
        //扩展Spring Security的用户身份信息呢
        return getUserPrincipal(user);
    }


    public UserDetails getUserPrincipal(XcUserExt user){
        //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
        //查询用户权限
        List<XcMenu> xcMenus = menuMapper.selectPermissionByUserId(user.getId());
        List<String> permissions = new ArrayList<>();
        if(xcMenus.size()<=0){
            //用户权限,如果不加则报Cannot pass a null GrantedAuthority collection
            permissions.add("p1");
        }else{
            xcMenus.forEach(menu->{
                permissions.add(menu.getCode());
            });
        }

        //将用户权限放在XcUserExt中
        user.setPermissions(permissions);
        String password = user.getPassword();
        //为了安全在令牌中不放密码
        user.setPassword(null);
        //将user对象转json
        String userString = JSON.toJSONString(user);
        String[] authorities = permissions.toArray(new String[0]);
        //创建UserDetails对象
        return User.withUsername(userString)
                .password(password)
                .authorities(authorities)
                .build();

    }
    //JwtAccessTokenConverter 会自动在 token 里生成
    //{
    //  "user_name": "{\"id\":\"123\",\"username\":\"zhangsan\",\"companyId\":\"xuecheng001\",\"permissions\":[\"p1\"]}",
    //  "authorities": ["p1"],
    //  "client_id": "XcWebApp",
    //  "scope": ["all"],
    //  "exp": 1726705200
    //}

}
