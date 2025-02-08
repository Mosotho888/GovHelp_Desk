package com.loggingsystem.springjwtauth.config.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {
    private final RabbitMQProperties properties;

    public RabbitMQConfig(RabbitMQProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.loggingsystem.springjwtauth.dto"); // Crucial!
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
    @Bean
    public Queue ticketAssignmentQueue() {
        return new Queue(properties.getTechnicianAssignmentQueue(), false);
    }

    @Bean
    public Queue ticketStatusChangeQueue() {
        return new Queue(properties.getTicketStatusChangeQueue(), false);
    }

    @Bean
    public Queue ticketCommentQueue() {
        return new Queue(properties.getTicketCommentQueue(), false);
    }

    @Bean
    public Queue ticketCreationQueue() {
        return new Queue(properties.getTicketCreationQueue(), false);
    }

    @Bean
    public DirectExchange ticketAssignmentExchange() {
        return new DirectExchange(properties.getTechnicianAssignmentExchange());
    }

    @Bean
    public DirectExchange ticketStatusChangeExchange() {
        return new DirectExchange(properties.getTicketStatusChangeExchange());
    }

    @Bean
    public DirectExchange ticketCommentExchange() {
        return new DirectExchange(properties.getTicketCommentExchange());
    }

    @Bean
    public DirectExchange ticketCreationExchange() {
        log.info("config TCEx: {}", properties.getTicketCreationExchange());
        return new DirectExchange(properties.getTicketCreationExchange());
    }

    @Bean
    public Binding ticketAssignmentBinding(Queue ticketAssignmentQueue, DirectExchange ticketAssignmentExchange) {
        return BindingBuilder.bind(ticketAssignmentQueue).to(ticketAssignmentExchange).with(properties.getTechnicianAssignedRoutingKey());
    }

    @Bean
    public Binding ticketStatusChangeBinding(Queue ticketStatusChangeQueue, DirectExchange ticketStatusChangeExchange) {
        return BindingBuilder.bind(ticketStatusChangeQueue).to(ticketStatusChangeExchange).with(properties.getTicketStatusChangedRoutingKey());
    }

    @Bean
    public Binding ticketCommentBinding(Queue ticketCommentQueue, DirectExchange ticketCommentExchange) {
        return BindingBuilder.bind(ticketCommentQueue).to(ticketCommentExchange).with(properties.getTicketCommentAddedRoutingKey());
    }

    @Bean
    public Binding ticketCreationBiding(Queue ticketCreationQueue, DirectExchange ticketCreationExchange) {
        return BindingBuilder.bind(ticketCreationQueue).to(ticketCreationExchange).with(properties.getTicketCreatedRoutingKey());
    }
}
