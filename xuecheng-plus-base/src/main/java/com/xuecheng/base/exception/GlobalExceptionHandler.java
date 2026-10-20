package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@ControllerAdvice  //若类上用了 @RestControllerAdvice，则不需要写 @ResponseBody（@RestControllerAdvice 已包含）
//@RestControllerAdvice
public class GlobalExceptionHandler {

 //对项目的自定义异常类型进行处理
   @ResponseBody //表示方法返回的 RestErrorResponse 将被序列化为 JSON（而不是去渲染页面）
   @ExceptionHandler(XueChengPlusException.class)
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
 public RestErrorResponse customException(XueChengPlusException e){

    //记录异常
    log.error("系统异常{}",e.getErrMessage(),e);
    //..

    //解析出异常信息
    String errMessage = e.getErrMessage();
       return new RestErrorResponse(errMessage);
   }


   @ResponseBody //表示方法返回的 RestErrorResponse 将被序列化为 JSON（而不是去渲染页面）
   @ExceptionHandler(Exception.class)
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
 public RestErrorResponse exception(Exception e){

    //记录异常
    log.error("系统异常{}",e.getMessage(),e);
       // e.printStackTrace();
       if(e.getMessage().equals("不允许访问")){
           return new RestErrorResponse("没有操作此功能的权限");
       }

    //解析出异常信息
    RestErrorResponse restErrorResponse = new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
    return restErrorResponse;
   }

   //MethodArgumentNotValidException
    @ResponseBody //表示方法返回的 RestErrorResponse 将被序列化为 JSON（而不是去渲染页面）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e){

        BindingResult bindingResult = e.getBindingResult();
        //执行校验失败时，bindingResult 里会有内容大致如下：
        //bindingResult.hasErrors() == true
        //bindingResult.getFieldErrors() = [
        //    FieldError{objectName='addCourseDto', field='name', rejectedValue='', defaultMessage='课程名称不能为空'},
        //    FieldError{objectName='addCourseDto', field='charge', rejectedValue=null, defaultMessage='收费规则不能为空'}
        //]
        //bindingResult.getAllErrors() = [
        //    ObjectError{objectName='addCourseDto', defaultMessage='课程名称不能为空'},
        //    ObjectError{objectName='addCourseDto', defaultMessage='收费规则不能为空'}
        //]
        //存储错误信息
        List<String> errors = new ArrayList<>();
        bindingResult.getFieldErrors().stream().forEach(item->{
            errors.add(item.getDefaultMessage());
        });

        //将list中的错误信息拼接起来
        String errMessage = StringUtils.join(errors, ",");
        //记录异常
        log.error("系统异常{}",e.getMessage(),errMessage);

        //解析出异常信息
        RestErrorResponse restErrorResponse = new RestErrorResponse(errMessage);
        return restErrorResponse;
    }
}
