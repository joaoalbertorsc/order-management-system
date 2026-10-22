package com.ordermanagement.order.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Reports whether the Kafka cluster is reachable, shown under "kafka" in /actuator/health.
 * Spring Boot ships no Kafka health check, and this service cannot accept orders while Kafka is down.
 */
@Component("kafka")
public class KafkaHealthIndicator implements HealthIndicator {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        try {
            DescribeClusterResult cluster = adminClient.describeCluster(
                    new DescribeClusterOptions().timeoutMs((int) TIMEOUT.toMillis()));
            String clusterId = cluster.clusterId().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            int nodes = cluster.nodes().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).size();
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodes", nodes)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Health.down(e).build();
        } catch (Exception e) {
            return Health.down(e).build();
        } finally {
            adminClient.close(Duration.ofSeconds(1));
        }
    }
}
