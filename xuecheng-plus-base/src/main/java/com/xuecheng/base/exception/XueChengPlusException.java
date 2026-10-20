package com.xuecheng.base.exception;


public class XueChengPlusException extends RuntimeException {   //这个类继承了 RuntimeException，意味着它是一个非受检异常。这允许你在代码中抛出它而不需要强制进行 try-catch 块处理，让代码更简洁。

    private String errMessage;

    public XueChengPlusException() {
    }

    public XueChengPlusException(String message) {
        super(message);
        this.errMessage = message;

    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    public static void cast(String message){
        throw new XueChengPlusException(message);
    }
    public static void cast(CommonError error){
        throw new XueChengPlusException(error.getErrMessage());
    }

}
