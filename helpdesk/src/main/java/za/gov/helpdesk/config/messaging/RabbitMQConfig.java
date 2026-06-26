package za.gov.helpdesk.config.messaging;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration infrastructure engine initializing the AMQP RabbitMQ messaging topology. Defines
 * enterprise system integration parameters including primary topic exchanges, business worker
 * queues, failure boundary dead letter routing definitions, explicit bindings, JSON codecs, and
 * concurrent listener thread container factories.
 */
@Configuration
@Slf4j
public class RabbitMQConfig {

    @Value("${app.rabbitmq.concurrent-consumers}")
    private int concurrentConsumers;

    @Value("${app.rabbitmq.max-concurrent-consumers}")
    private int maxConcurrentConsumers;

    /**
     * Declares the main {@link TopicExchange} where all operational and system notifications are
     * initially dispatched by publisher components.
     *
     * @return the primary business message topic exchange
     */
    @Bean
    public TopicExchange helpdeskExchange() {
        return new TopicExchange(RabbitMQConstants.EXCHANGE);
    }

    /**
     * Declares the dedicated Dead Letter {@link TopicExchange} (DLX) used to trap, aggregate, and
     * route discarded messages that fail transactional verification constraints.
     *
     * @return the secondary error recovery dead letter exchange
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(RabbitMQConstants.DLX);
    }

    /**
     * Establishes a durable tracking queue to retain unrecoverable audit trail logging event
     * messages.
     *
     * @return a durable audit log dead letter queue wrapper
     */
    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(RabbitMQConstants.AUDIT_DLQ).build();
    }

    /**
     * Establishes a durable tracking queue to retain unrecoverable password reset notification
     * messages.
     *
     * @return a durable password reset email dead letter queue wrapper
     */
    @Bean
    public Queue passwordResetEmailDlq() {
        return QueueBuilder.durable(RabbitMQConstants.PASSWORD_RESET_EMAIL_DLQ).build();
    }

    /**
     * Establishes a durable tracking queue to retain unrecoverable ticket notification messages.
     *
     * @return a durable ticket email dead letter queue wrapper
     */
    @Bean
    public Queue ticketEmailDlq() {
        return QueueBuilder.durable(RabbitMQConstants.TICKET_EMAIL_DLQ).build();
    }

    /**
     * Establishes a durable tracking queue to retain unrecoverable SLA escalation alert messages.
     *
     * @return a durable SLA email dead letter queue wrapper
     */
    @Bean
    public Queue slaEmailDlq() {
        return QueueBuilder.durable(RabbitMQConstants.SLA_EMAIL_DLQ).build();
    }

    /**
     * Establishes a durable worker queue dedicated to picking up operational auditing messages.
     * Hooks up dead-letter parameters to guarantee toxic messages fall back seamlessly to the DLX.
     *
     * @return a pre-configured durable audit worker queue
     */
    @Bean
    public Queue auditQueue() {

        return QueueBuilder.durable(RabbitMQConstants.AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.AUDIT_DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * Establishes a durable worker queue dedicated to processing outbound security password reset
     * notifications. Hooks up dead-letter parameters to guarantee toxic messages fall back
     * seamlessly to the DLX.
     *
     * @return a pre-configured durable password reset worker queue
     */
    @Bean
    public Queue passwordResetEmailQueue() {

        return QueueBuilder.durable(RabbitMQConstants.PASSWORD_RESET_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                .withArgument(
                        "x-dead-letter-routing-key",
                        RabbitMQConstants.PASSWORD_RESET_EMAIL_DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * Establishes a durable worker queue dedicated to processing standard outbound ticket lifecycle
     * notifications. Hooks up dead-letter parameters to guarantee toxic messages fall back
     * seamlessly to the DLX.
     *
     * @return a pre-configured durable ticket notification worker queue
     */
    @Bean
    public Queue ticketEmailQueue() {

        return QueueBuilder.durable(RabbitMQConstants.TICKET_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                .withArgument(
                        "x-dead-letter-routing-key", RabbitMQConstants.TICKET_EMAIL_DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * Establishes a durable worker queue dedicated to processing critical time-sensitive SLA
     * escalation notifications. Hooks up dead-letter parameters to guarantee toxic messages fall
     * back seamlessly to the DLX.
     *
     * @return a pre-configured durable SLA notification worker queue
     */
    @Bean
    public Queue slaEmailQueue() {

        return QueueBuilder.durable(RabbitMQConstants.SLA_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                .withArgument(
                        "x-dead-letter-routing-key", RabbitMQConstants.SLA_EMAIL_DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * Interconnects the main audit logging queue to the central topic exchange using an exclusive
     * routing pattern key.
     *
     * @param auditQueue the targeted worker destination queue
     * @param helpdeskExchange the central dispatching exchange source
     * @return a fully populated network communication routing {@link Binding} link
     */
    @Bean
    public Binding bindAuditQueue(final Queue auditQueue, final TopicExchange helpdeskExchange) {
        return BindingBuilder.bind(auditQueue)
                .to(helpdeskExchange)
                .with(RabbitMQConstants.AUDIT_ROUTING_KEY);
    }

    /**
     * Interconnects the ticket notification queue to the central topic exchange using an exclusive
     * routing pattern key.
     *
     * @param ticketEmailQueue the targeted worker destination queue
     * @param helpdeskExchange the central dispatching exchange source
     * @return a fully populated network communication routing {@link Binding} link
     */
    @Bean
    public Binding bindTicketEmailQueue(
            final Queue ticketEmailQueue, final TopicExchange helpdeskExchange) {
        return BindingBuilder.bind(ticketEmailQueue)
                .to(helpdeskExchange)
                .with(RabbitMQConstants.TICKET_EMAIL_ROUTING_KEY);
    }

    /**
     * Interconnects the SLA alert queue to the central topic exchange using an exclusive routing
     * pattern key.
     *
     * @param slaEmailQueue the targeted worker destination queue
     * @param helpdeskExchange the central dispatching exchange source
     * @return a fully populated network communication routing {@link Binding} link
     */
    @Bean
    public Binding bindSlaEmailQueue(
            final Queue slaEmailQueue, final TopicExchange helpdeskExchange) {
        return BindingBuilder.bind(slaEmailQueue)
                .to(helpdeskExchange)
                .with(RabbitMQConstants.SLA_EMAIL_ROUTING_KEY);
    }

    /**
     * Interconnects the security password reset queue to the central topic exchange using an
     * exclusive routing pattern key.
     *
     * @param passwordResetEmailQueue the targeted worker destination queue
     * @param helpdeskExchange the central dispatching exchange source
     * @return a fully populated network communication routing {@link Binding} link
     */
    @Bean
    public Binding bindPasswordResetEmailQueue(
            final Queue passwordResetEmailQueue, final TopicExchange helpdeskExchange) {
        return BindingBuilder.bind(passwordResetEmailQueue)
                .to(helpdeskExchange)
                .with(RabbitMQConstants.PASSWORD_RESET_EMAIL_ROUTING_KEY);
    }

    /**
     * Maps the dedicated error-handling audit log DLQ directly into the Dead Letter Exchange
     * topology.
     *
     * @param auditDlq the error aggregation container queue
     * @param deadLetterExchange the isolation fallback exchange source
     * @return a fully populated fault-tolerance routing {@link Binding} link
     */
    @Bean
    public Binding bindAuditDlq(final Queue auditDlq, final TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(auditDlq)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.AUDIT_DLQ_ROUTING_KEY);
    }

    /**
     * Maps the error-handling ticket notification DLQ directly into the Dead Letter Exchange
     * topology.
     *
     * @param ticketEmailDlq the error aggregation container queue
     * @param deadLetterExchange the isolation fallback exchange source
     * @return a fully populated fault-tolerance routing {@link Binding} link
     */
    @Bean
    public Binding bindTicketEmailDlq(
            final Queue ticketEmailDlq, final TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(ticketEmailDlq)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.TICKET_EMAIL_DLQ_ROUTING_KEY);
    }

    /**
     * Maps the error-handling SLA alert DLQ directly into the Dead Letter Exchange topology.
     *
     * @param slaEmailDlq the error aggregation container queue
     * @param deadLetterExchange the isolation fallback exchange source
     * @return a fully populated fault-tolerance routing {@link Binding} link
     */
    @Bean
    public Binding bindSlaEmailDlq(
            final Queue slaEmailDlq, final TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(slaEmailDlq)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.SLA_EMAIL_DLQ_ROUTING_KEY);
    }

    /**
     * Maps the error-handling password reset DLQ directly into the Dead Letter Exchange topology.
     *
     * @param passwordResetEmailDlq the error aggregation container queue
     * @param deadLetterExchange the isolation fallback exchange source
     * @return a fully populated fault-tolerance routing {@link Binding} link
     */
    @Bean
    public Binding bindPasswordResetEmailDlq(
            final Queue passwordResetEmailDlq, final TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(passwordResetEmailDlq)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.PASSWORD_RESET_EMAIL_DLQ_ROUTING_KEY);
    }

    /**
     * Instantiates a Jackson JSON message converter bean capable of handling complex serialization
     * profiles. Safely processes modern Java 8 time frames and defines clear package security trust
     * boundaries.
     *
     * @return a customized {@link Jackson2JsonMessageConverter} serialization engine
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        final Jackson2JsonMessageConverter converter =
                new Jackson2JsonMessageConverter(objectMapper);

        final DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("za.gov.helpdesk"); // Crucial!
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    /**
     * Configures the template framework used for transactional message generation. Injects the
     * application JSON marshaller to guarantee uniform outbound structured formatting.
     *
     * @param connectionFactory the underlying connection broker infrastructure
     * @param jsonMessageConverter the system marshalling strategy codec
     * @return an explicit pre-configured {@link RabbitTemplate} dispatching manager bean
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            final ConnectionFactory connectionFactory,
            final Jackson2JsonMessageConverter jsonMessageConverter) {

        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);

        return rabbitTemplate;
    }

    /**
     * Overrides and instantiates the core asynchronous message listener factory. Enforces explicit
     * thread scalability boundaries and applies strict manual message acknowledgment gates.
     *
     * @param connectionFactory the underlying connection broker infrastructure
     * @param jsonMessageConverter the system unmarshalling strategy codec
     * @return an customized {@link SimpleRabbitListenerContainerFactory} engine manager bean
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            final ConnectionFactory connectionFactory,
            final Jackson2JsonMessageConverter jsonMessageConverter) {

        final SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);

        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        return factory;
    }
}
