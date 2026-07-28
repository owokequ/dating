package com.dating.owoke.core;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka-native:4.1.0");
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17");
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:8.2.1");

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(KAFKA_IMAGE);
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(REDIS_IMAGE)
                .withCommand("redis-server", "--appendonly", "yes", "--requirepass", "dating-redis")
                .withExposedPorts(6379);
    }
}
