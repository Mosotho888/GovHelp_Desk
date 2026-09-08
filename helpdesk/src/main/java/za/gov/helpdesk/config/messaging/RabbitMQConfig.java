package za.gov.helpdesk.config.messaging;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
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
     * Declares the primary business topic exchange and the Dead Letter Exchange (DLX) used to trap
     * messages that fail transactional verification constraints.
     *
     * @return the {@link Declarables} wrapper bundling both topic exchanges
     */
    @Bean
    public Declarables topicExchanges() {
        return new Declarables(
                new TopicExchange(RabbitMQConstants.EXCHANGE),
                new TopicExchange(RabbitMQConstants.DLX));
    }

    /**
     * Establishes the durable dead-letter queues that retain unrecoverable audit, ticket, password
     * reset, and SLA notification messages.
     *
     * @return the {@link Declarables} wrapper bundling every dead letter queue
     */
    @Bean
    public Declarables deadLetterQueues() {
        return new Declarables(
                QueueBuilder.durable(RabbitMQConstants.AUDIT_DLQ).build(),
                QueueBuilder.durable(RabbitMQConstants.PASSWORD_RESET_EMAIL_DLQ).build(),
                QueueBuilder.durable(RabbitMQConstants.TICKET_EMAIL_DLQ).build(),
                QueueBuilder.durable(RabbitMQConstants.SLA_EMAIL_DLQ).build());
    }

    /**
     * Establishes the durable worker queues for audit, ticket, password reset, and SLA
     * notifications, each wired with dead-letter parameters so toxic messages fall back seamlessly
     * to the DLX.
     *
     * @return the {@link Declarables} wrapper bundling every worker queue
     */
    @Bean
    public Declarables workerQueues() {
        return new Declarables(
                QueueBuilder.durable(RabbitMQConstants.AUDIT_QUEUE)
                        .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                        .withArgument(
                                "x-dead-letter-routing-key",
                                RabbitMQConstants.AUDIT_DLQ_ROUTING_KEY)
                        .build(),
                QueueBuilder.durable(RabbitMQConstants.PASSWORD_RESET_EMAIL_QUEUE)
                        .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                        .withArgument(
                                "x-dead-letter-routing-key",
                                RabbitMQConstants.PASSWORD_RESET_EMAIL_DLQ_ROUTING_KEY)
                        .build(),
                QueueBuilder.durable(RabbitMQConstants.TICKET_EMAIL_QUEUE)
                        .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                        .withArgument(
                                "x-dead-letter-routing-key",
                                RabbitMQConstants.TICKET_EMAIL_DLQ_ROUTING_KEY)
                        .build(),
                QueueBuilder.durable(RabbitMQConstants.SLA_EMAIL_QUEUE)
                        .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX)
                        .withArgument(
                                "x-dead-letter-routing-key",
                                RabbitMQConstants.SLA_EMAIL_DLQ_ROUTING_KEY)
                        .build());
    }

    /**
     * Binds every worker queue to the main topic exchange and every dead letter queue to the DLX,
     * each using its dedicated routing key.
     *
     * @return the {@link Declarables} wrapper bundling every binding in the topology
     */
    @Bean
    public Declarables bindings() {
        return new Declarables(
                bindingOf(
                        RabbitMQConstants.AUDIT_QUEUE,
                        RabbitMQConstants.EXCHANGE,
                        RabbitMQConstants.AUDIT_ROUTING_KEY),
                bindingOf(
                        RabbitMQConstants.TICKET_EMAIL_QUEUE,
                        RabbitMQConstants.EXCHANGE,
                        RabbitMQConstants.TICKET_EMAIL_ROUTING_KEY),
                bindingOf(
                        RabbitMQConstants.SLA_EMAIL_QUEUE,
                        RabbitMQConstants.EXCHANGE,
                        RabbitMQConstants.SLA_EMAIL_ROUTING_KEY),
                bindingOf(
                        RabbitMQConstants.PASSWORD_RESET_EMAIL_QUEUE,
                        RabbitMQConstants.EXCHANGE,
                        RabbitMQConstants.PASSWORD_RESET_EMAIL_ROUTING_KEY),
                bindingOf(
                        RabbitMQConstants.AUDIT_DLQ,
                        RabbitMQConstants.DLX,
                        RabbitMQConstants.AUDIT_DLQ_ROUTING_KEY),
                bindingOf(
                        RabbitMQConstants.TICKET_EMAIL_DLQ,
                        RabbitMQConstants.DLX,
                        RabbitMQConstants.TICKET_EMAIL_DLQ_ROUTING_KEY),
                bindingOf(
                        RabbitMQConstants.SLA_EMAIL_DLQ,
                        RabbitMQConstants.DLX,
                        RabbitMQConstants.SLA_EMAIL_DLQ_ROUTING_KEY),
                bindingOf(
                        RabbitMQConstants.PASSWORD_RESET_EMAIL_DLQ,
                        RabbitMQConstants.DLX,
                        RabbitMQConstants.PASSWORD_RESET_EMAIL_DLQ_ROUTING_KEY));
    }

    private static Binding bindingOf(
            final String queueName, final String exchangeName, final String routingKey) {
        return new Binding(
                queueName, Binding.DestinationType.QUEUE, exchangeName, routingKey, null);
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
