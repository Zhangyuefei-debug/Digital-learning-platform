package com.xuecheng.checkcode.service.impl;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.xuecheng.base.utils.EncryptUtil;
import com.xuecheng.checkcode.model.CheckCodeParamsDto;
import com.xuecheng.checkcode.model.CheckCodeResultDto;
import com.xuecheng.checkcode.service.AbstractCheckCodeService;
import com.xuecheng.checkcode.service.CheckCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Base64;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


@Service("PicCheckCodeService")
public class PicCheckCodeServiceImpl extends AbstractCheckCodeService implements CheckCodeService {


    @Autowired
    private DefaultKaptcha kaptcha;

    //设置生成器
    @Resource(name="NumberLetterCheckCodeGenerator")
    @Override
    public void setCheckCodeGenerator(CheckCodeGenerator checkCodeGenerator) {
        this.checkCodeGenerator = checkCodeGenerator;
    }

    //设置key生成器
    @Resource(name="UUIDKeyGenerator")
    @Override
    public void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }


    //设置存储器
    @Resource(name="RedisCheckCodeStore")
    @Override
    public void setCheckCodeStore(CheckCodeStore checkCodeStore) {
        this.checkCodeStore = checkCodeStore;
    }


    //生成验证码
    @Override
    public CheckCodeResultDto generate(CheckCodeParamsDto checkCodeParamsDto) {// 继承自 AbstractCheckCodeService，从而间接实现了 CheckCodeService 接口。


                                                                                                                        //generate(CheckCodeParamsDto checkCodeParamsDto) 方法
                                                                                                                        //对于 generate 方法：
                                                                                                                        //CheckCodeService 接口定义了方法签名。
                                                                                                                        //AbstractCheckCodeService 将其声明为 public abstract CheckCodeResultDto generate(...)。
                                                                                                                        //在这种情况下，PicCheckCodeServiceImpl 必须重写（实现）这个方法，因为它从 AbstractCheckCodeService 中继承了一个抽象方法。
                                                                                                                        //重写目标： PicCheckCodeServiceImpl 实际上是在实现 AbstractCheckCodeService 中定义的抽象方法，同时也实现了 CheckCodeService 接口中的同名方法。
        GenerateResult generate = generate(checkCodeParamsDto, 4, "checkcode:", 60);//调用通用逻辑： 它首先调用了抽象类中的通用骨架方法 generate(checkCodeParamsDto, 4, "checkcode:", 60)，获取到通用的 key 和 code。
        String key = generate.getKey();
        String code = generate.getCode();
        String pic = createPic(code);  //提供独有细节： 接着，它执行了该子类独有的逻辑——createPic(code)，将生成的验证码字符串转换为图片（Base64 编码）。
        CheckCodeResultDto checkCodeResultDto = new CheckCodeResultDto();
        checkCodeResultDto.setAliasing(pic);
        checkCodeResultDto.setKey(key);
        return checkCodeResultDto;

    }

    //创建图片
//    private String createPic(String code) {
//        // 生成图片验证码
//        ByteArrayOutputStream outputStream = null;
//        BufferedImage image = kaptcha.createImage(code);
//
//        outputStream = new ByteArrayOutputStream();
//        String imgBase64Encoder = null;
//        try {
//            // 对字节数组Base64编码
//            BASE64Encoder base64Encoder = new BASE64Encoder();
//            ImageIO.write(image, "png", outputStream);
//            imgBase64Encoder = "data:image/png;base64," + EncryptUtil.encodeBase64(outputStream.toByteArray());
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                outputStream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return imgBase64Encoder;
//    }



    private String createPic(String code) {
        // 使用 try-with-resources 自动关闭流
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 生成验证码图片
            BufferedImage image = kaptcha.createImage(code);

            // 写入字节流
            ImageIO.write(image, "png", outputStream);

            // 使用标准Base64编码
            String base64 = Base64.getEncoder()
                    .encodeToString(outputStream.toByteArray());

            // 拼接成前端可直接使用的格式
            return "data:image/png;base64," + base64;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
