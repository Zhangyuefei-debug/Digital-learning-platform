package com.xuecheng.orders.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuecheng.orders.config.OrderCancelConfig;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcPayRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class OrderCancelListener {

    @Autowired
    private XcOrdersMapper ordersMapper;

    @Autowired
    private XcPayRecordMapper payRecordMapper;

    /**
     * 监听超时未支付订单，执行取消操作
     */
    @Transactional
    @RabbitListener(queues = OrderCancelConfig.ORDER_CANCEL_DEAD_QUEUE)
    public void handleOrderCancel(String orderIdStr) {
        Long orderId = Long.valueOf(orderIdStr);
        log.info("接收到超时订单取消消息：orderId={}", orderId);

        XcOrders order = ordersMapper.selectById(orderId);
        if (order == null) {
            log.warn("订单不存在：{}", orderId);
            return;
        }

        // 如果已支付则不处理
        if ("600002".equals(order.getStatus())) {
            log.info("订单已支付，无需取消：{}", orderId);
            return;
        }

        // 条件更新：只有未支付(600001)才允许取消，避免与支付流程竞态覆盖
        int rows = ordersMapper.update(null, new LambdaUpdateWrapper<XcOrders>()
                .eq(XcOrders::getId, orderId)
                .eq(XcOrders::getStatus, "600001")
                .set(XcOrders::getStatus, "600003"));
        if (rows == 0) {
            log.info("订单状态已变更，无需取消：{}", orderId);
            return;
        }

        // 条件更新支付记录：只有未支付(601001)才置为过期
        payRecordMapper.update(null, new LambdaUpdateWrapper<XcPayRecord>()
                .eq(XcPayRecord::getOrderId, orderId)
                .eq(XcPayRecord::getStatus, "601001")
                .set(XcPayRecord::getStatus, "601003"));

        //更新优惠券信息

        log.info("订单超时取消完成：{}", orderId);
    }
}
