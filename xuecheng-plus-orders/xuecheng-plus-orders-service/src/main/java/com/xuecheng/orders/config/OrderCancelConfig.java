package com.xuecheng.orders.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单取消延迟队列配置（TTL + DLX）
 */
@Configuration
public class OrderCancelConfig {

    // 延迟交换机
    public static final String ORDER_CANCEL_EXCHANGE = "order_cancel_exchange";

    // 延迟队列（设置TTL）
    public static final String ORDER_CANCEL_DELAY_QUEUE = "order_cancel_delay_queue";

    // 死信交换机（消息过期后路由到这里）
    public static final String ORDER_CANCEL_DEAD_EXCHANGE = "order_cancel_dead_exchange";

    // 死信队列（真正被监听的队列）
    public static final String ORDER_CANCEL_DEAD_QUEUE = "order_cancel_dead_queue";

    // 延迟消息过期后由此路由键转发到死信交换机
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel";

    /** 延迟交换机（普通交换机） */
    @Bean
    public Exchange orderCancelExchange() {
        return ExchangeBuilder.directExchange(ORDER_CANCEL_EXCHANGE).durable(true).build();
    }

    /** 延迟队列：设置TTL并绑定死信交换机 */
    @Bean
    public Queue orderCancelDelayQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_DELAY_QUEUE)
                // 过期后发往死信交换机
                .withArgument("x-dead-letter-exchange", ORDER_CANCEL_DEAD_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_CANCEL_ROUTING_KEY)
                // 设置延迟时间（30分钟）
                .withArgument("x-message-ttl", 30 * 60 * 1000)
                .build();
    }

    /** 延迟队列绑定到延迟交换机 */
    @Bean
    public Binding delayQueueBinding() {
        return BindingBuilder.bind(orderCancelDelayQueue())
                .to(orderCancelExchange())
                .with("order.delay")
                .noargs();
    }

    /** 死信交换机 */
    @Bean
    public Exchange orderCancelDeadExchange() {
        return ExchangeBuilder.directExchange(ORDER_CANCEL_DEAD_EXCHANGE).durable(true).build();
    }

    /** 死信队列 */
    @Bean
    public Queue orderCancelDeadQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_DEAD_QUEUE).build();
    }


    /** 死信队列绑定到死信交换机 */
    @Bean
    public Binding deadQueueBinding() {
        return BindingBuilder.bind(orderCancelDeadQueue())
                .to(orderCancelDeadExchange())
                .with(ORDER_CANCEL_ROUTING_KEY)
                .noargs();
    }
}

//我们通过 TTL + 死信交换机实现延迟消息。
//订单创建后先发送到一个设置了 TTL 的延迟队列，消息在队列中不被消费；
//TTL 到期后 RabbitMQ 自动将消息转发到死信交换机，路由到真正被监听的死信队列，
//消费者从死信队列中处理订单取消逻辑，整个过程无需阻塞线程，可靠性和可扩展性都很好。