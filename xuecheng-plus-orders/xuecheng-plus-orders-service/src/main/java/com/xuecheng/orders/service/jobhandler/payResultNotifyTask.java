package com.xuecheng.orders.service.jobhandler;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.service.OrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class payResultNotifyTask {

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    OrderService orderService;


    @XxlJob("payResultNotifyJob")
    public void payResultNotifyJob() {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex="+shardIndex+",shardTotal="+shardTotal);

        List<MqMessage> list = mqMessageService.getMessageList(shardIndex, shardTotal, "payresult_notify",30);
        for (MqMessage message : list) {
            try {
                orderService.notifyPayResult(message);
            } catch (Exception e) {
                log.error("支付结果通知重发失败，messageId={}", message.getId(), e);
            }
        }
    }
}
