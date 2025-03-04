package com.loggingsystem.springjwtauth.emailnotification.service.impl.message;

import com.loggingsystem.springjwtauth.config.messaging.RabbitMQProperties;
import com.loggingsystem.springjwtauth.emailnotification.dto.EmailNotificationDTO;
import com.loggingsystem.springjwtauth.emailnotification.service.MessageSenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageSenderServiceImpl implements MessageSenderService {
    private final AmqpTemplate amqpTemplate;
    private final RabbitMQProperties properties;

    public MessageSenderServiceImpl(AmqpTemplate amqpTemplate, RabbitMQProperties properties) {
        this.amqpTemplate = amqpTemplate;
        this.properties = properties;
    }

    @Override
    public void sendTechnicianAssignmentMessage(EmailNotificationDTO notification) {
        log.info("Exchange: {} RoutingKey: {}", properties.getTechnicianAssignmentExchange(), properties.getTechnicianAssignedRoutingKey());
        amqpTemplate.convertAndSend(properties.getTechnicianAssignmentExchange(), properties.getTechnicianAssignedRoutingKey(), notification);
    }

    @Override
    public void sendTicketStatusChangeMessage(EmailNotificationDTO notification) {
        log.info("Exchange: {} RoutingKey: {}", properties.getTicketStatusChangeExchange(), properties.getTicketStatusChangedRoutingKey());
        amqpTemplate.convertAndSend(properties.getTicketStatusChangeExchange(), properties.getTicketStatusChangedRoutingKey(), notification);
    }

    @Override
    public void sendTicketCommentMessage(EmailNotificationDTO notification) {
        log.info("Exchange: {} RoutingKey: {}", properties.getTicketCommentExchange(), properties.getTicketCommentAddedRoutingKey());
        amqpTemplate.convertAndSend(properties.getTicketCommentExchange(), properties.getTicketCommentAddedRoutingKey(), notification);
    }

    @Override
    public void sendTicketCreationMessage(EmailNotificationDTO notification) {
        log.info("Exchange: {} RoutingKey: {}", properties.getTicketCreationExchange(), properties.getTicketCreatedRoutingKey());
        amqpTemplate.convertAndSend(properties.getTicketCreationExchange(), properties.getTicketCreatedRoutingKey(), notification);
    }

}
