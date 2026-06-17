package za.gov.helpdesk.config.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {

    @Value("${app.rabbitmq.concurrent-consumers}")
    private int concurrentConsumers;

    @Value("${app.rabbitmq.max-concurrent-consumers}")
    private int maxConcurrentConsumers;

    @Bean
    public TopicExchange helpdeskExchange() {
        return new TopicExchange(RabbitMQConstants.EXCHANGE);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(RabbitMQConstants.DLX);
    }

    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(RabbitMQConstants.AUDIT_DLQ).build();
    }

    @Bean
    public Queue passwordResetEmailDlq() {
        return QueueBuilder.durable(RabbitMQConstants.PASSWORD_RESET_EMAIL_DLQ).build();
    }

    @Bean
    public Queue ticketEmailDlq() {
        return QueueBuilder.durable(RabbitMQConstants.TICKET_EMAIL_DLQ).build();
    }

    @Bean
    public Queue slaEmailDlq() {
        return QueueBuilder.durable(RabbitMQConstants.SLA_EMAIL_DLQ).build();
    }

    @Bean
    public Queue auditQueue() {

        return QueueBuilder.durable(RabbitMQConstants.AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.AUDIT_DLQ_ROUTING_KEY).build();
    }

    @Bean
    public Queue passwordResetEmailQueue() {

        return QueueBuilder.durable(RabbitMQConstants.PASSWORD_RESET_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.PASSWORD_RESET_EMAIL_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue ticketEmailQueue() {

        return QueueBuilder.durable(RabbitMQConstants.TICKET_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.TICKET_EMAIL_DLQ_ROUTING_KEY).build();
    }

    @Bean
    public Queue slaEmailQueue() {

        return QueueBuilder.durable(RabbitMQConstants.SLA_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.SLA_EMAIL_DLQ_ROUTING_KEY).build();
    }

    @Bean
    public Binding bindAuditQueue(Queue auditQueue, TopicExchange helpdeskExchange) {
        return BindingBuilder.bind(auditQueue).to(helpdeskExchange).with(RabbitMQConstants.AUDIT_ROUTING_KEY);
    }

    @Bean
    public Binding bindTicketEmailQueue(Queue ticketEmailQueue, TopicExchange helpdeskExchange) {
        return BindingBuilder.bind(ticketEmailQueue).to(helpdeskExchange)
                .with(RabbitMQConstants.TICKET_EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding bindSlaEmailQueue(Queue slaEmailQueue, TopicExchange helpdeskExchange) {
        return BindingBuilder.bind(slaEmailQueue).to(helpdeskExchange).with(RabbitMQConstants.SLA_EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding bindPasswordResetEmailQueue(Queue passwordResetEmailQueue, TopicExchange helpdeskExchange) {
        return BindingBuilder.bind(passwordResetEmailQueue).to(helpdeskExchange)
                .with(RabbitMQConstants.PASSWORD_RESET_EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding bindAuditDlq(Queue auditDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(auditDlq).to(deadLetterExchange).with(RabbitMQConstants.AUDIT_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding bindTicketEmailDlq(Queue ticketEmailDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(ticketEmailDlq).to(deadLetterExchange)
                .with(RabbitMQConstants.TICKET_EMAIL_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding bindSlaEmailDlq(Queue slaEmailDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(slaEmailDlq).to(deadLetterExchange)
                .with(RabbitMQConstants.SLA_EMAIL_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding bindPasswordResetEmailDlq(Queue passwordResetEmailDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(passwordResetEmailDlq).to(deadLetterExchange)
                .with(RabbitMQConstants.PASSWORD_RESET_EMAIL_DLQ_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("za.gov.helpdesk"); // Crucial!
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jsonMessageConverter) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);

        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);

        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        return factory;
    }
}
