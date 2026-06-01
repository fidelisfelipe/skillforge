package com.skillforge.hero.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfig {

    static final String QUEUE                        = "hero-template.problems";
    static final String KEY                          = "problem.#";
    static final String KATA_RESPONSES_QUEUE         = "kata.responses";
    static final String KATA_VALIDATION_RESULTS_QUEUE = "kata.validation.results";

    @Value("${guild.amqp.exchange:skillforge}")
    private String exchangeName;

    @Value("${skillforge.hero.id:hero-template}")
    private String heroId;

    @Bean
    TopicExchange skillforgeExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue problemQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding problemBinding(TopicExchange skillforgeExchange) {
        return BindingBuilder.bind(problemQueue()).to(skillforgeExchange).with(KEY);
    }

    @Bean
    Queue kataResponsesQueue() {
        return QueueBuilder.durable(KATA_RESPONSES_QUEUE).build();
    }

    @Bean
    Queue kataValidationResultsQueue() {
        return QueueBuilder.durable(KATA_VALIDATION_RESULTS_QUEUE).build();
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter(ObjectMapper mapper) {
        var converter = new Jackson2JsonMessageConverter(mapper);
        // REQUIRED: use the @RabbitListener parameter type, not the __TypeId__ header sent by hub.
        // Without this, deserialization fails with ClassNotFoundException on the hub's package.
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    Queue revisionQueue() {
        return QueueBuilder.durable(heroId + ".revisions").build();
    }

    @Bean
    Binding revisionBinding(TopicExchange skillforgeExchange) {
        return BindingBuilder.bind(revisionQueue()).to(skillforgeExchange).with("revision." + heroId);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
        var t = new RabbitTemplate(cf);
        t.setMessageConverter(conv);
        return t;
    }
}