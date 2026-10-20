package com.xuecheng.checkcode.service;

import com.xuecheng.checkcode.model.CheckCodeParamsDto;
import com.xuecheng.checkcode.model.CheckCodeResultDto;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;



//实现了验证码生成和校验的通用流程 (generate 和 verify 方法)，但将具体的实现细节（如如何生成码、如何存取）委托给子组件。实现了验证码校验和删除验证码的通用逻辑。
@Slf4j
public abstract class AbstractCheckCodeService implements CheckCodeService {

    protected CheckCodeGenerator checkCodeGenerator;
    protected KeyGenerator keyGenerator;
    protected CheckCodeStore checkCodeStore;

    public abstract void  setCheckCodeGenerator(CheckCodeGenerator checkCodeGenerator);
    public abstract void  setKeyGenerator(KeyGenerator keyGenerator);
    public abstract void  setCheckCodeStore(CheckCodeStore CheckCodeStore);


    public GenerateResult generate(CheckCodeParamsDto checkCodeParamsDto,Integer code_length,String keyPrefix,Integer expire){
        //数字字母生成器，生成四位验证码
        String code = checkCodeGenerator.generate(code_length);
        log.debug("生成验证码:{}",code);
        //生成一个key
        String key = keyGenerator.generate(keyPrefix);

        //存储验证码
        checkCodeStore.set(key,code,expire);
        //返回验证码生成结果
        GenerateResult generateResult = new GenerateResult();
        generateResult.setKey(key);
        generateResult.setCode(code);
        return generateResult;
    }

    @Data
    protected class GenerateResult{
        String key;
        String code;
    }


    // 不实现generate方法，而是将其声明为抽象方法
    //如果一个抽象类在实现接口时，没有实现 接口中的所有方法（或一个都没实现），那么它必须将这些未实现的方法声明为 抽象方法（Abstract Method）。这是因为抽象类本身可以包含抽象方法。  如果一个抽象类实现了接口中的所有方法，它就不再包含任何抽象方法（至少来自这个接口）。
    public abstract CheckCodeResultDto generate(CheckCodeParamsDto checkCodeParamsDto);  //将接口中定义的 CheckCodeResultDto generate(CheckCodeParamsDto) 方法声明为 abstract。这意味着它将如何将通用结果 (GenerateResult) 转化为 CheckCodeResultDto 的责任推给了子类。
    //举例说明 (Java/C# 语言的常见情况):
    //假设有一个接口 Flyable 有一个方法 fly()。
    //抽象类 Bird 实现 Flyable:
    //Java
    //抽象类将责任推给子类
    //abstract class Bird implements Flyable {
    //    不实现 fly()，而是将其声明为抽象方法
    //    public abstract void fly();
    //    ... 其他方法
    //}
    //非抽象子类 Eagle 继承 Bird:
    //Java
    //非抽象子类必须实现所有继承的抽象方法
    //class Eagle extends Bird {
    //    @Override
    //    public void fly() {
    //        提供具体的实现
    //        System.out.println("Eagle is flying high.");
    //    }
    //}
    //这样，抽象类 就可以作为一个中间层，部分地实现接口功能，并定义一个通用的骨架，而将细节的实现留给它的具体子类去完成。

    //校验验证码
    public boolean verify(String key, String code){
        if (StringUtils.isBlank(key) || StringUtils.isBlank(code)){
            return false;
        }
        String code_l = checkCodeStore.get(key);
        if (code_l == null){
            return false;
        }
        boolean result = code_l.equalsIgnoreCase(code);
        if(result){
            //删除验证码
            checkCodeStore.remove(key);
        }
        return result;
    }


}
