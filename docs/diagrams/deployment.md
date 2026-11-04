# Deployment (Docker Compose)

`docker compose up -d --build` starts six containers on one Docker network.

```mermaid
flowchart TB
    subgraph host[Your machine]
        browser([Browser / curl / IDE])

        subgraph compose["Docker network: order-management-system_default"]
            order["oms-order-service<br/>8081"]
            processing["oms-processing-service<br/>8082"]
            print["oms-print-service<br/>8083"]
            kafka[["oms-kafka<br/>kafka:29092 (internal)<br/>localhost:9092 (host)"]]
            mongo[("oms-mongodb<br/>27017<br/>volume mongo-data")]
            kafkaui["oms-kafka-ui<br/>8090 -> 8080"]
        end
    end

    browser -- ":8081" --> order
    browser -- ":8082" --> processing
    browser -- ":8083 actuator" --> print
    browser -- ":8090" --> kafkaui
    browser -. ":9092 / :27017<br/>services run from the IDE" .-> kafka
    browser -.-> mongo

    order -- "kafka:29092" --> kafka
    processing -- "kafka:29092" --> kafka
    print -- "kafka:29092" --> kafka
    processing -- "mongodb:27017" --> mongo
    kafkaui -- "kafka:29092" --> kafka
```

## Startup order

Docker health checks control the startup order:

1. `kafka` and `mongodb` start first. Kafka is healthy once its broker API answers. MongoDB is healthy once `ping` succeeds.
2. The services start only after their dependencies are healthy (`depends_on: condition: service_healthy`).
3. Each service is healthy once `/actuator/health/readiness` returns `UP`.

## Kafka listeners

Kafka advertises two addresses:

| Listener | Address | Used by |
|---|---|---|
| `PLAINTEXT` | `kafka:29092` | Containers on the Docker network |
| `PLAINTEXT_HOST` | `localhost:9092` | Applications running on the host (e.g. the IDE) |

The services read the address from the `KAFKA_BOOTSTRAP_SERVERS` environment variable. Its default is `localhost:9092`, and Docker Compose sets it to `kafka:29092`. MongoDB works the same way through `MONGODB_URI`.

## Images

Each service image is built in two stages: Maven builds the jar, then only that jar is copied onto a small Java 21 runtime image (`eclipse-temurin:21-jre-alpine`). The container runs as a non-root user.
