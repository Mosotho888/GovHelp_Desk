package com.loggingsystem.springjwtauth.component;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.rabbitmq")
@Data
public class RabbitMQProperties {
    private String technicianAssignmentQueue;
    private String ticketStatusChangeQueue;
    private String ticketCommentQueue;
    private String ticketCreationQueue;

    private String technicianAssignmentExchange;
    private String ticketStatusChangeExchange;
    private String ticketCommentExchange;
    private String ticketCreationExchange;

    private String technicianAssignedRoutingKey;
    private String ticketStatusChangedRoutingKey;
    private String ticketCommentAddedRoutingKey;
    private String ticketCreatedRoutingKey;
}
