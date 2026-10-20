package com.xuecheng.checkcode.service;

import com.xuecheng.checkcode.model.CheckCodeParamsDto;
import com.xuecheng.checkcode.model.CheckCodeResultDto;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.Map;




//定义了验证码服务的核心功能 (generate 和 verify)，以及三个子组件的接口：CheckCodeGenerator，验证码生成器、KeyGenerator，key生成器、CheckCodeStore，验证码存储。
public interface CheckCodeService {



     CheckCodeResultDto generate(CheckCodeParamsDto checkCodeParamsDto);


    public boolean verify(String key, String code);


    public interface CheckCodeGenerator{
        /**
         * 验证码生成
         * @return 验证码
         */
        String generate(int length);
        

    }


    public interface KeyGenerator{

        /**
         * key生成
         * @return 验证码
         */
        String generate(String prefix);
    }


    /**
     * @description 验证码存储
     * @author Mr.M
     * @date 2022/9/29 16:34
     */
    public interface CheckCodeStore{


        void set(String key, String value, Integer expire);  //它只规定了“存储”这个动作，但没说“如何存储”。

        String get(String key);

        void remove(String key);
    }
}
