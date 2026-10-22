# order-service components

How a `POST /orders` request flows through the classes. The service layer depends only on interfaces (`OrderValidationRule`, `OrderEventPublisher`), so the HTTP and Kafka details stay at the edges.

```mermaid
flowchart LR
    client([Client])

    subgraph api[api]
        controller[OrderController]
        dto["CreateOrderRequest<br/>OrderResponse"]
        handler[GlobalExceptionHandler]
    end

    subgraph service[service]
        orderService[OrderService]
    end

    subgraph validation[validation]
        ruleIf{{"OrderValidationRule<br/>(interface)"}}
        unique[UniqueProductRule]
        max[MaxTotalAmountRule]
    end

    subgraph messaging[messaging]
        publisherIf{{"OrderEventPublisher<br/>(interface)"}}
        kafkaPublisher[KafkaOrderEventPublisher]
        event[OrderCreatedEvent]
    end

    subgraph domain[domain]
        order["Order / OrderItem / Customer"]
    end

    kafka[["Kafka topic pedidos"]]

    client -- "JSON" --> controller
    controller -- "validates with @Valid" --> dto
    controller --> orderService
    orderService -- "Order.create(...)" --> order
    orderService --> ruleIf
    unique -. implements .-> ruleIf
    max -. implements .-> ruleIf
    orderService --> publisherIf
    kafkaPublisher -. implements .-> publisherIf
    kafkaPublisher -- "maps to" --> event
    kafkaPublisher -- "KafkaTemplate.send" --> kafka
    controller -. "exceptions" .-> handler
    handler -- "400 / 422 / 503 / 500" --> client
```

| Layer | Responsibility | Knows about |
|---|---|---|
| `api` | Turns HTTP into calls and exceptions into HTTP | Spring MVC, DTOs |
| `service` | The "create order" use case | Domain and the two interfaces |
| `validation` | One business rule per class | Domain |
| `messaging` | Turns a domain order into a Kafka message | Kafka, the event contract |
| `domain` | Order data and the total calculation | Nothing else |
