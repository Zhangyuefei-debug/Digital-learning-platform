package com.xuecheng.content.feignclient;

import com.xuecheng.content.config.MultipartSupportConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;


//定义一个fallback类MediaServiceClientFallback，此类实现了MediaServiceClient接口。
//上述方法无法取出熔断所抛出的异常，第二种方法定义MediaServiceClientFallbackFactory 可以解决这个问题。
@FeignClient(value = "media-api",configuration = {MultipartSupportConfig.class},fallbackFactory = MediaServiceClientFallbackFactory.class)
public interface MediaServiceClient {


    @RequestMapping(value = "/media/upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String uploadFile(@RequestPart("filedata") MultipartFile upload, @RequestParam(value = "objectName",required=false) String objectName);


}

//1️⃣ MultipartSupportConfig 的作用
//
//在 Feign 中：
//
//默认 Feign 不支持文件上传（MultipartFile）。
//
//因为普通的 Feign Encoder 只能处理 JSON 或普通表单，不知道如何把 MultipartFile 转成 multipart/form-data。
//
//所以我们需要自定义一个 Feign 配置类，告诉 Feign：
//
//“遇到 MultipartFile 或 multipart/form-data 表单时，用 Spring 的 SpringFormEncoder 处理。”
//例子：
//@Configuration
//public class MultipartSupportConfig {
//    @Bean
//    public Encoder feignFormEncoder() {
//        return new SpringFormEncoder();
//    }
//}
//这个 Encoder 会把 MultipartFile 和普通参数自动封装成 HTTP 的 multipart/form-data 格式。
//然后 Feign 才能正确上传文件。
//所以在你的代码里：
//@FeignClient(value = "media-api", configuration = MultipartSupportConfig.class)
//就相当于告诉 Feign：“这个 Client 需要特殊处理文件上传”。
//✅ 如果你的接口不涉及文件上传，就不需要配置 MultipartSupportConfig。




//2️⃣ 调用支付服务
//
//假设你要调用一个支付服务，不上传文件，只是普通 JSON 请求：
//@FeignClient(value = "payment-service") // 不需要配置 MultipartSupportConfig
//public interface PaymentClient {
//
//    @PostMapping("/payment/payOrder")
//    String payOrder(@RequestBody PaymentRequest request);
//}
//value = "payment-service" → 注册中心里的支付服务名称。
//@PostMapping("/payment/payOrder") → 支付接口路径。
//@RequestBody PaymentRequest request → 普通 JSON 请求体。
//返回 String → 返回支付结果（可以是 JSON、状态码等）。
//⚠️ 注意：这里 没有 configuration，因为不涉及 multipart/form-data。