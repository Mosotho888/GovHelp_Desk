package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.component.RabbitMQProperties;
import com.loggingsystem.springjwtauth.dto.EmailNotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageSender {
    private final AmqpTemplate amqpTemplate;
    private final RabbitMQProperties properties;

    public MessageSender(AmqpTemplate amqpTemplate, RabbitMQProperties properties) {
        this.amqpTemplate = amqpTemplate;
        this.properties = properties;
    }

    public void sendTechnicianAssignmentMessage(EmailNotificationDTO notification) {
        log.info("Exchange: {} RoutingKey: {}", properties.getTechnicianAssignmentExchange(), properties.getTechnicianAssignedRoutingKey());
        amqpTemplate.convertAndSend(properties.getTechnicianAssignmentExchange(), properties.getTechnicianAssignedRoutingKey(), notification);
    }

    public void sendTicketStatusChangeMessage(EmailNotificationDTO notification) {
        log.info("Exchange: {} RoutingKey: {}", properties.getTicketStatusChangeExchange(), properties.getTicketStatusChangedRoutingKey());
        amqpTemplate.convertAndSend(properties.getTicketStatusChangeExchange(), properties.getTicketStatusChangedRoutingKey(), notification);
    }

    public void sendTicketCommentMessage(EmailNotificationDTO notification) {
        log.info("Exchange: {} RoutingKey: {}", properties.getTicketCommentExchange(), properties.getTicketCommentAddedRoutingKey());
        amqpTemplate.convertAndSend(properties.getTicketCommentExchange(), properties.getTicketCommentAddedRoutingKey(), notification);
    }

    public void sendTicketCreationMessage(EmailNotificationDTO notification) {
        log.info("Exchange: {} RoutingKey: {}", properties.getTicketCreationExchange(), properties.getTicketCreatedRoutingKey());
        amqpTemplate.convertAndSend(properties.getTicketCreationExchange(), properties.getTicketCreatedRoutingKey(), notification);
    }

}
