package cn.monitor4all.logRecord.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yangzhendong
 */
@Data
@ConfigurationProperties(prefix = "log-record")
public class LogRecordProperties {

    private RabbitMqProperties rabbitMqProperties;

    private RocketMqProperties rocketMqProperties;

    /**
     * spring cloud stream 配置
     */
    private StreamProperties stream;

    /**
     * choose pipeline for message: rabbitMq, rocketMq, stream
     */
    private String dataPipeline;

    @Data
    public static class RabbitMqProperties {
        private String addresses;
        private String host;
        private int port;
        private String username;
        private String password;
        private String queueName = "logRecord";
        private String exchangeName = "logRecord";
        private String routingKey = "logRecord";
    }

    @Data
    public static class RocketMqProperties {
        private String topic = "logRecord";
        private String tag = "";
        private String namesrvAddr = "localhost:9876";
        private String groupName = "logRecord";
        private int maxMessageSize = 4000000;
        private int sendMsgTimeout = 3000;
        private int retryTimesWhenSendFailed = 2;
    }

    @Data
    public class StreamProperties {

        /**
         * 默认对应消息中间件topic rocketmq的topic, RabbitMq的 exchangeName
         */
        private String destination;

        /**
         * 默认对应的分组
         */
        private String group;

        /**
         * 默认的binder（对应的消息中间件）
         */
        private String binder;
    }

}
