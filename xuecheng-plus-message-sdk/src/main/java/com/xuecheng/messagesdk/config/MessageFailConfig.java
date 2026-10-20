package com.xuecheng.messagesdk.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageFailConfig {
    public static final String FAIL_QUEUE="fail.queue";

    public static final String FAIL_EXCHANGE="fail.exchange";

    public static final String FAIL_ROUTING_KEY="fail.routing.key";

    @Bean
    public Queue failQueue(){
        return new Queue(FAIL_QUEUE,true);
    }

    @Bean
    public DirectExchange failExchange(){
        return new DirectExchange(FAIL_EXCHANGE,true,false);
    }

    @Bean
    public Binding failBinding(Queue failQueue, DirectExchange failExchange){
        return BindingBuilder.bind(failQueue).to(failExchange).with(FAIL_ROUTING_KEY);
    }

}
