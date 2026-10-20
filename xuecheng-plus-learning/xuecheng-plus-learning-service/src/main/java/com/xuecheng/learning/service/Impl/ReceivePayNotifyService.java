package com.xuecheng.learning.service.Impl;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Slf4j
@Service
public class ReceivePayNotifyService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    MyCourseTablesService myCourseTablesService;


    //监听消息队列接收支付结果通知
    //Confirm/Return 管的是“消息进没进队列”。一旦 broker 确认 ack，订单服务的 completed(id) 就会把 mq_message 表那条记录删掉。所以消费端如果失败，那张表已经不再兜底了，得靠队列自己的重投/死信机制来保证。
    //消费端的 ACK 模型
    //消息投递给消费者后，broker 会等消费者回答：
    //basicAck：这条处理成功，从队列删除。
    //basicNack/basicReject + requeue=true：放回原队列，之后再投一次。
    //basicNack/basicReject + requeue=false：不回到原队列；队列配置了死信交换机就进死信队列，没配就直接丢弃。
    //业务成功：方法正常返回，Spring 自动 ack，消息删除。
    //业务失败：抛异常，消息自动 requeue，下一次再投给消费者，失败多少次就重试多少次。
    //优点是有“至少一次”的保证；缺点是失败消息会无限循环，消费端不成功队列就一直卡着，而且这种重试是同一队列内无退避地重投。
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message, Channel channel) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //获取消息
        MqMessage mqMessage = JSON.parseObject(message.getBody(), MqMessage.class);
        log.debug("学习中心服务接收支付结果:{}", mqMessage);

        //消息类型
        String messageType = mqMessage.getMessageType();
        //订单类型,60201表示购买课程
        String businessKey2 = mqMessage.getBusinessKey2();
        //这里只处理支付结果通知
        if (PayNotifyConfig.MESSAGE_TYPE.equals(messageType) && "60201".equals(businessKey2)) {
            //选课记录id
            String choosecourseId = mqMessage.getBusinessKey1();
            //添加选课
            boolean b = myCourseTablesService.saveChooseCourseStauts(choosecourseId);
            if(!b){
                //添加选课失败，抛出异常，消息重回队列
                XueChengPlusException.cast("收到支付结果，添加选课失败");
            }
        }
    }

//    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
//    public void receive(Message message, Channel channel) throws IOException {
//
//        long deliveryTag = message.getMessageProperties().getDeliveryTag();
//        deliveryTag 是 broker 给“本次投递”分配的编号，ack/nack 时要用它告诉 broker 说的是哪条消息；false 表示只处理这一条，不批量确认。
//
//        try {
//            MqMessage mqMessage = JSON.parseObject(message.getBody(), MqMessage.class);
//            log.debug("学习中心服务接收支付结果:{}", mqMessage);
//
//            if (PayNotifyConfig.MESSAGE_TYPE.equals(mqMessage.getMessageType())
//                    && "60201".equals(mqMessage.getBusinessKey2())) {
//
//                boolean b = myCourseTablesService.saveChooseCourseStauts(mqMessage.getBusinessKey1());
//
//                if (!b) {
//                    throw new RuntimeException("添加选课失败");
//                }
//            }
//            // ✅ 业务成功，手动 ACK
//            channel.basicAck(deliveryTag, false); //basicAck(deliveryTag, false)：业务处理成功，手动确认，broker 删消息。
//
//        } catch (Exception e) {
//            log.error("消费失败", e);
//
//            // ❌ 不重回队列，进入死信队列
//            // 处理失败，requeue=false 不送回原队列。如果队列配了死信交换机，消息进死信队列，相当于“先放一边，等人排查”；如果没配，消息会被丢弃。
//            channel.basicNack(deliveryTag, false, false);
//        }
//    }

}
