package za.gov.helpdesk.config.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
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
    public Queue emailDlq() {
        return QueueBuilder.durable(RabbitMQConstants.EMAIL_DLQ).build();
    }

    @Bean
    public Queue auditQueue() {

        return QueueBuilder.durable(RabbitMQConstants.AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.AUDIT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue emailQueue() {

        return QueueBuilder.durable(RabbitMQConstants.EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.EMAIL_DLQ_ROUTING_KEY).build();
    }

    @Bean
    public Binding bindAuditQueue(Queue auditQueue, TopicExchange helpdeskExchange) {
        return BindingBuilder.bind(auditQueue).to(helpdeskExchange).with(RabbitMQConstants.AUDIT_ROUTING_KEY);
    }

    @Bean
    public Binding bindEmailQueue(Queue emailQueue, TopicExchange helpdeskExchange) {
        return BindingBuilder.bind(emailQueue).to(helpdeskExchange).with(RabbitMQConstants.EMAIL_ROUTING_KEY);
    }
    @Bean
    public Binding bindAuditDlq(Queue auditDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(auditDlq)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.AUDIT_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding bindEmailDlq(Queue emailDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(emailDlq)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.EMAIL_DLQ_ROUTING_KEY);
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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jsonMessageConverter) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);

        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
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
