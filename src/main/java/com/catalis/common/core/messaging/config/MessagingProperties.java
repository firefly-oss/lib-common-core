package com.catalis.common.core.messaging.config;

import com.catalis.common.core.messaging.serialization.SerializationFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for messaging.
 */
@Configuration
@ConfigurationProperties(prefix = "messaging")
@Getter
@Setter
public class MessagingProperties {

    /**
     * Whether to enable messaging features.
     * Disabled by default.
     */
    private boolean enabled = false;

    /**
     * Whether to enable resilience features (circuit breaker, retry, metrics).
     */
    private boolean resilience = true;

    /**
     * Default timeout for publishing operations in seconds.
     */
    private int publishTimeoutSeconds = 5;

    /**
     * Application name to use as the source service in message headers.
     * If not specified, it will try to use spring.application.name.
     */
    private String applicationName;

    /**
     * Serialization configuration.
     */
    private SerializationConfig serialization = new SerializationConfig();

    /**
     * Kafka configuration.
     */
    private KafkaConfig kafka = new KafkaConfig();

    /**
     * RabbitMQ configuration.
     */
    private RabbitMqConfig rabbitmq = new RabbitMqConfig();

    /**
     * Amazon SQS configuration.
     */
    private SqsConfig sqs = new SqsConfig();

    /**
     * Google Cloud Pub/Sub configuration.
     */
    private GooglePubSubConfig googlePubSub = new GooglePubSubConfig();

    /**
     * Azure Service Bus configuration.
     */
    private AzureServiceBusConfig azureServiceBus = new AzureServiceBusConfig();

    /**
     * Redis Pub/Sub configuration.
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * JMS (ActiveMQ) configuration.
     */
    private JmsConfig jms = new JmsConfig();

    /**
     * AWS Kinesis configuration.
     */
    private KinesisConfig kinesis = new KinesisConfig();

    /**
     * Kafka configuration properties.
     */
    @Getter
    @Setter
    public static class KafkaConfig {
        /**
         * Whether to enable Kafka publishing.
         */
        private boolean enabled = false;

        /**
         * Default Kafka topic to use if not specified in the annotation.
         */
        private String defaultTopic = "events";

        /**
         * Bootstrap servers for Kafka (comma-separated list).
         */
        private String bootstrapServers = "localhost:9092";

        /**
         * Client ID for Kafka producer.
         */
        private String clientId = "messaging-publisher";

        /**
         * Key serializer class for Kafka producer.
         */
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";

        /**
         * Value serializer class for Kafka producer.
         */
        private String valueSerializer = "org.apache.kafka.common.serialization.StringSerializer";

        /**
         * Security protocol for Kafka.
         * Possible values: PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL
         */
        private String securityProtocol = "PLAINTEXT";

        /**
         * SASL mechanism for Kafka.
         * Possible values: PLAIN, SCRAM-SHA-256, SCRAM-SHA-512, GSSAPI, OAUTHBEARER
         */
        private String saslMechanism = "";

        /**
         * SASL username for Kafka.
         */
        private String saslUsername = "";

        /**
         * SASL password for Kafka.
         */
        private String saslPassword = "";

        /**
         * Additional properties for Kafka producer.
         */
        private Map<String, String> properties = new HashMap<>();
    }

    /**
     * RabbitMQ configuration properties.
     */
    @Getter
    @Setter
    public static class RabbitMqConfig {
        /**
         * Whether to enable RabbitMQ publishing.
         */
        private boolean enabled = false;

        /**
         * Default RabbitMQ exchange to use if not specified in the annotation.
         */
        private String defaultExchange = "events";

        /**
         * Default RabbitMQ queue to use if not specified in the annotation.
         */
        private String defaultQueue = "events";

        /**
         * Default RabbitMQ routing key to use if not specified in the annotation.
         */
        private String defaultRoutingKey = "default";

        /**
         * RabbitMQ host.
         */
        private String host = "localhost";

        /**
         * RabbitMQ port.
         */
        private int port = 5672;

        /**
         * RabbitMQ virtual host.
         */
        private String virtualHost = "/";

        /**
         * RabbitMQ username.
         */
        private String username = "guest";

        /**
         * RabbitMQ password.
         */
        private String password = "guest";

        /**
         * Whether to use SSL for RabbitMQ connection.
         */
        private boolean ssl = false;

        /**
         * Connection timeout in milliseconds.
         */
        private int connectionTimeout = 60000;

        /**
         * Prefetch count for RabbitMQ consumers.
         */
        private int prefetchCount = 10;

        /**
         * Whether to use durable queues.
         */
        private boolean durable = true;

        /**
         * Whether to use exclusive queues.
         */
        private boolean exclusive = false;

        /**
         * Whether to auto-delete queues.
         */
        private boolean autoDelete = false;

        /**
         * Additional properties for RabbitMQ connection.
         */
        private Map<String, String> properties = new HashMap<>();
    }

    /**
     * Amazon SQS configuration properties.
     */
    @Getter
    @Setter
    public static class SqsConfig {
        /**
         * Whether to enable SQS publishing.
         */
        private boolean enabled = false;

        /**
         * Default SQS queue name to use if not specified in the annotation.
         */
        private String defaultQueue = "events";

        /**
         * Region for the SQS client.
         */
        private String region = "us-east-1";

        /**
         * AWS access key ID.
         */
        private String accessKeyId = "";

        /**
         * AWS secret access key.
         */
        private String secretAccessKey = "";

        /**
         * AWS session token (for temporary credentials).
         */
        private String sessionToken = "";

        /**
         * SQS endpoint override (useful for localstack or custom endpoints).
         */
        private String endpoint = "";

        /**
         * Maximum number of messages to retrieve in a single batch.
         */
        private int maxNumberOfMessages = 10;

        /**
         * Visibility timeout in seconds.
         */
        private int visibilityTimeout = 30;

        /**
         * Wait time in seconds for long polling.
         */
        private int waitTimeSeconds = 20;

        /**
         * Additional properties for SQS client.
         */
        private Map<String, String> properties = new HashMap<>();
    }

    /**
     * Google Cloud Pub/Sub configuration properties.
     */
    @Getter
    @Setter
    public static class GooglePubSubConfig {
        /**
         * Whether to enable Google Pub/Sub publishing.
         */
        private boolean enabled = false;

        /**
         * Default topic to use if not specified in the annotation.
         */
        private String defaultTopic = "events";

        /**
         * Project ID for Google Cloud.
         */
        private String projectId = "";

        /**
         * Path to the Google Cloud credentials JSON file.
         */
        private String credentialsPath = "";

        /**
         * Google Cloud credentials JSON as a string.
         */
        private String credentialsJson = "";

        /**
         * Endpoint override for Google Pub/Sub (useful for emulator).
         */
        private String endpoint = "";

        /**
         * Whether to use the emulator.
         */
        private boolean useEmulator = false;

        /**
         * Emulator host (e.g., localhost:8085).
         */
        private String emulatorHost = "";

        /**
         * Retry settings - initial retry delay in milliseconds.
         */
        private long initialRetryDelayMillis = 100;

        /**
         * Retry settings - retry delay multiplier.
         */
        private double retryDelayMultiplier = 1.3;

        /**
         * Retry settings - maximum retry delay in milliseconds.
         */
        private long maxRetryDelayMillis = 60000;

        /**
         * Retry settings - maximum number of attempts.
         */
        private int maxAttempts = 5;

        /**
         * Additional properties for Google Pub/Sub client.
         */
        private Map<String, String> properties = new HashMap<>();
    }

    /**
     * Azure Service Bus configuration properties.
     */
    @Getter
    @Setter
    public static class AzureServiceBusConfig {
        /**
         * Whether to enable Azure Service Bus publishing.
         */
        private boolean enabled = false;

        /**
         * Default topic to use if not specified in the annotation.
         */
        private String defaultTopic = "events";

        /**
         * Default queue to use if not specified in the annotation.
         */
        private String defaultQueue = "events";

        /**
         * Connection string for Azure Service Bus.
         */
        private String connectionString = "";

        /**
         * Namespace for Azure Service Bus.
         */
        private String namespace = "";

        /**
         * Shared access key name for Azure Service Bus.
         */
        private String sharedAccessKeyName = "RootManageSharedAccessKey";

        /**
         * Shared access key for Azure Service Bus.
         */
        private String sharedAccessKey = "";

        /**
         * Whether to use managed identity for authentication.
         */
        private boolean useManagedIdentity = false;

        /**
         * Client ID for managed identity.
         */
        private String clientId = "";

        /**
         * Retry settings - maximum number of attempts.
         */
        private int maxRetries = 3;

        /**
         * Retry settings - delay in milliseconds.
         */
        private long retryDelayMillis = 100;

        /**
         * Retry settings - maximum delay in milliseconds.
         */
        private long maxRetryDelayMillis = 30000;

        /**
         * Retry settings - delay multiplier.
         */
        private double retryDelayMultiplier = 1.5;

        /**
         * Additional properties for Azure Service Bus client.
         */
        private Map<String, String> properties = new HashMap<>();
    }

    /**
     * Redis Pub/Sub configuration properties.
     */
    @Getter
    @Setter
    public static class RedisConfig {
        /**
         * Whether to enable Redis Pub/Sub publishing.
         */
        private boolean enabled = false;

        /**
         * Default channel to use if not specified in the annotation.
         */
        private String defaultChannel = "events";

        /**
         * Redis host.
         */
        private String host = "localhost";

        /**
         * Redis port.
         */
        private int port = 6379;

        /**
         * Redis password.
         */
        private String password = "";

        /**
         * Redis database index.
         */
        private int database = 0;

        /**
         * Connection timeout in milliseconds.
         */
        private int timeout = 2000;

        /**
         * Whether to use SSL for Redis connection.
         */
        private boolean ssl = false;

        /**
         * Redis sentinel master name (if using Redis Sentinel).
         */
        private String sentinelMaster = "";

        /**
         * Redis sentinel nodes (comma-separated list, if using Redis Sentinel).
         */
        private String sentinelNodes = "";

        /**
         * Redis cluster nodes (comma-separated list, if using Redis Cluster).
         */
        private String clusterNodes = "";

        /**
         * Maximum number of redirects for Redis Cluster.
         */
        private int maxRedirects = 3;

        /**
         * Additional properties for Redis connection.
         */
        private Map<String, String> properties = new HashMap<>();
    }

    /**
     * JMS (ActiveMQ) configuration properties.
     */
    @Getter
    @Setter
    public static class JmsConfig {
        /**
         * Whether to enable JMS publishing.
         */
        private boolean enabled = false;

        /**
         * Default destination to use if not specified in the annotation.
         */
        private String defaultDestination = "events";

        /**
         * Whether to use topics (true) or queues (false).
         */
        private boolean useTopic = true;

        /**
         * JMS broker URL (e.g., tcp://localhost:61616 for ActiveMQ).
         */
        private String brokerUrl = "tcp://localhost:61616";

        /**
         * JMS username.
         */
        private String username = "";

        /**
         * JMS password.
         */
        private String password = "";

        /**
         * Client ID for JMS connection.
         */
        private String clientId = "messaging-publisher";

        /**
         * Connection factory class name.
         */
        private String connectionFactoryClass = "org.apache.activemq.ActiveMQConnectionFactory";

        /**
         * Whether to use transactions.
         */
        private boolean transacted = false;

        /**
         * Acknowledgement mode.
         * 1 = AUTO_ACKNOWLEDGE
         * 2 = CLIENT_ACKNOWLEDGE
         * 3 = DUPS_OK_ACKNOWLEDGE
         * 4 = SESSION_TRANSACTED
         */
        private int acknowledgeMode = 1;

        /**
         * Connection timeout in milliseconds.
         */
        private int connectionTimeout = 30000;

        /**
         * Whether to use SSL for JMS connection.
         */
        private boolean ssl = false;

        /**
         * Trust store path for SSL.
         */
        private String trustStorePath = "";

        /**
         * Trust store password for SSL.
         */
        private String trustStorePassword = "";

        /**
         * Key store path for SSL.
         */
        private String keyStorePath = "";

        /**
         * Key store password for SSL.
         */
        private String keyStorePassword = "";

        /**
         * Additional properties for JMS connection.
         */
        private Map<String, String> properties = new HashMap<>();
    }

    /**
     * AWS Kinesis configuration properties.
     */
    @Getter
    @Setter
    public static class KinesisConfig {
        /**
         * Whether to enable Kinesis publishing.
         */
        private boolean enabled = false;

        /**
         * Default Kinesis stream to use if not specified in the annotation.
         */
        private String defaultStream = "events";

        /**
         * AWS region for Kinesis.
         */
        private String region = "us-east-1";

        /**
         * AWS access key ID.
         */
        private String accessKeyId = "";

        /**
         * AWS secret access key.
         */
        private String secretAccessKey = "";

        /**
         * AWS session token (for temporary credentials).
         */
        private String sessionToken = "";

        /**
         * Kinesis endpoint override (useful for localstack or custom endpoints).
         */
        private String endpoint = "";

        /**
         * Maximum number of records to retrieve in a single batch.
         */
        private int maxRecords = 100;

        /**
         * Initial position in the stream when starting a new consumer.
         * Valid values: LATEST, TRIM_HORIZON, AT_TIMESTAMP
         */
        private String initialPosition = "LATEST";

        /**
         * Timestamp to use when initialPosition is AT_TIMESTAMP (ISO-8601 format).
         */
        private String initialTimestamp = "";

        /**
         * Shard iterator type for consumer.
         * Valid values: AT_SEQUENCE_NUMBER, AFTER_SEQUENCE_NUMBER, AT_TIMESTAMP, TRIM_HORIZON, LATEST
         */
        private String shardIteratorType = "LATEST";

        /**
         * Application name for Kinesis Client Library (KCL).
         */
        private String applicationName = "messaging-consumer";

        /**
         * Whether to use enhanced fan-out for Kinesis consumers.
         */
        private boolean enhancedFanOut = false;

        /**
         * Consumer name for enhanced fan-out.
         */
        private String consumerName = "messaging-consumer";

        /**
         * Retry settings - maximum number of attempts.
         */
        private int maxRetries = 3;

        /**
         * Retry settings - delay in milliseconds.
         */
        private long retryDelayMillis = 1000;

        /**
         * Additional properties for Kinesis client.
         */
        private Map<String, String> properties = new HashMap<>();
    }

    /**
     * Serialization configuration properties.
     */
    @Getter
    @Setter
    public static class SerializationConfig {
        /**
         * Default serialization format to use if not specified.
         */
        private SerializationFormat defaultFormat = SerializationFormat.JSON;

        /**
         * Whether to include type information in the serialized data.
         * <p>
         * This is useful for deserializing polymorphic types, but it may not be
         * supported by all serialization formats.
         */
        private boolean includeTypeInfo = false;

        /**
         * Whether to pretty-print JSON output.
         */
        private boolean prettyPrint = false;

        /**
         * Schema registry URL for Avro serialization.
         */
        private String schemaRegistryUrl = "";

        /**
         * Whether to use schema validation for Avro and Protobuf serialization.
         */
        private boolean validateSchema = true;

        /**
         * Additional properties for serialization.
         */
        private Map<String, String> properties = new HashMap<>();
    }
}
