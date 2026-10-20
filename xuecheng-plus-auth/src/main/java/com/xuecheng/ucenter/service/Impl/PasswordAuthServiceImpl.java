package com.xuecheng.ucenter.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

        @Autowired
        XcUserMapper xcUserMapper;

        @Autowired
        PasswordEncoder passwordEncoder;

        @Autowired
        CheckCodeClient checkCodeClient;


        @Override
        public XcUserExt execute(AuthParamsDto authParamsDto){

            //账号
            String username = authParamsDto.getUsername();
            //前端输入的验证码
            String checkcode = authParamsDto.getCheckcode();
            //验证码key
            String checkcodekey = authParamsDto.getCheckcodekey();
            if(StringUtils.isEmpty(checkcode)||StringUtils.isEmpty(checkcodekey)){
                throw new RuntimeException("请输入验证码");
            }
            //调用远程验证码服务校验验证码
            Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
            if(!(verify != null && verify)){
                throw new RuntimeException("验证码校验失败");
            }




            XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
            if (user == null) {
                //返回空表示用户不存在
                throw new RuntimeException("账号不存在");
            }
            XcUserExt xcUserExt = new XcUserExt();
            BeanUtils.copyProperties(user, xcUserExt);
            //校验密码
            //取出数据库存储的正确密码
            String passwordDb = user.getPassword();
            String passwordForm = authParamsDto.getPassword();
            boolean matches = passwordEncoder.matches(passwordForm, passwordDb);  //参数一是前端登录时输入的明文密码，参数二是数据库存储的密文密码
            if (!matches) {
                throw new RuntimeException("账号或密码错误");
            }
            return xcUserExt;

    }
}