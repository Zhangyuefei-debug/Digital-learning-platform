package com.xuecheng.content.service.impl;

import com.xuecheng.messagesdk.config.MessageFailConfig;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageFailListener {


    @RabbitListener(queues = MessageFailConfig.FAIL_QUEUE)
    public void onFailMessage(MqMessage mqMessage) {
        log.info("接收到失败消息：{}", mqMessage);
        //todo 发送短信或者邮件等人工介入处理
    }
}
