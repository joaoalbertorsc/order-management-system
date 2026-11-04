# System overview

All the components and how they connect. Solid arrows are the order flow, dotted arrows are monitoring.

```mermaid
flowchart LR
    client([Client / HTTP])

    subgraph services[Microservices]
        order["order-service<br/>:8081<br/>REST API + Kafka producer"]
        processing["processing-service<br/>:8082<br/>Kafka consumer + REST API"]
        print["print-service<br/>:8083<br/>Kafka consumer"]
    end

    subgraph infra[Infrastructure]
        kafka[["Kafka<br/>topic pedidos<br/>3 partitions"]]
        mongo[("MongoDB<br/>db order_management<br/>collection orders")]
        kafkaui["Kafka UI<br/>:8090"]
    end

    console[/Console output/]
    actuator["Spring Boot Actuator<br/>health, info, metrics"]

    client -- "POST /orders" --> order
    order -- "publish OrderCreatedEvent<br/>key = orderId" --> kafka
    kafka -- "consumer group processing-service" --> processing
    kafka -- "consumer group print-service" --> print
    processing -- "insert / find / update" --> mongo
    print -- "receipt" --> console
    client -- "GET /orders<br/>GET /orders/{id}<br/>PATCH /orders/{id}/status" --> processing
    kafkaui -.-> kafka

    order -.-> actuator
    processing -.-> actuator
    print -.-> actuator
```

Key points:

- The only link between the services is the `pedidos` topic. No service calls another directly.
- Both consumers get **every** order because they belong to different consumer groups.
- The processing-service owns the MongoDB data, so it also serves the read API.
