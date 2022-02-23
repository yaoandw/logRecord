package cn.monitor4all.logRecord.configuration;

import cn.monitor4all.logRecord.constans.LogConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * @author yangzhendong
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "log-record.data-pipeline", havingValue = LogConstants.DataPipeline.RABBIT_MQ)
@EnableConfigurationProperties({LogRecordProperties.class})
public class RabbitMqSenderConfiguration {

    private String rabbitAddresses;
    private String rabbitHost;
    private int rabbitPort;
    private String exchange;
    private String queue;
    private String routingKey;
    private String username;
    private String password;

    @Autowired
    private LogRecordProperties properties;

    @PostConstruct
    public void rabbitMqConfig() {
        this.rabbitAddresses = properties.getRabbitMqProperties().getAddresses();
        this.rabbitHost = properties.getRabbitMqProperties().getHost();
        this.rabbitPort = properties.getRabbitMqProperties().getPort();
        this.queue = properties.getRabbitMqProperties().getQueueName();
        this.routingKey = properties.getRabbitMqProperties().getRoutingKey();
        this.exchange= properties.getRabbitMqProperties().getExchangeName();
        this.username= properties.getRabbitMqProperties().getUsername();
        this.password= properties.getRabbitMqProperties().getPassword();
        log.info("LogRecord RabbitMqSenderConfiguration addresses [{}] host [{}] port [{}] exchange [{}] queue [{}] routingKey [{}]",
                rabbitAddresses, rabbitHost, rabbitPort, exchange, queue, routingKey);
    }

    @Bean
    ConnectionFactory rabbitConnectionFactory() {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        if (StringUtils.hasLength(rabbitAddresses)) {
            cachingConnectionFactory.setAddresses(rabbitAddresses);
        }else {
            cachingConnectionFactory.setHost(rabbitHost);
            cachingConnectionFactory.setPort(rabbitPort);
        }
        cachingConnectionFactory.setUsername(username);
        cachingConnectionFactory.setPassword(password);
        return cachingConnectionFactory;
    }

    @Bean
    DirectExchange rubeExchange() {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    public Queue rubeQueue() {
        return new Queue(queue, true);
    }

    @Bean
    Binding rubeExchangeBinding(DirectExchange rubeExchange, Queue rubeQueue) {
        return BindingBuilder.bind(rubeQueue).to(rubeExchange).with(routingKey);
    }

    @Bean
    public RabbitTemplate rubeExchangeTemplate(ConnectionFactory rabbitConnectionFactory) {
        RabbitTemplate r = new RabbitTemplate(rabbitConnectionFactory);
        r.setExchange(exchange);
        r.setRoutingKey(routingKey);
        r.setConnectionFactory(rabbitConnectionFactory);
        return r;
    }
}
