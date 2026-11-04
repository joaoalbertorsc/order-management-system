# Create order flow

A new order, from the HTTP request until it is stored and printed.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant OS as order-service
    participant K as Kafka topic pedidos
    participant PS as processing-service
    participant DB as MongoDB
    participant PR as print-service

    Client->>OS: POST /orders
    OS->>OS: Bean Validation (fields)
    OS->>OS: business rules (unique products, max total)
    OS->>OS: build order (UUID, total, createdAt)
    OS->>K: send OrderCreatedEvent, key = orderId
    K-->>OS: ack (partition, offset)
    OS-->>Client: 202 Accepted + order id and total

    par consumer group processing-service
        K->>PS: OrderCreatedEvent
        PS->>PS: validate event
        PS->>DB: insert order, status CREATED
        DB-->>PS: ok
        PS->>K: commit offset
    and consumer group print-service
        K->>PR: OrderCreatedEvent
        PR->>PR: format receipt and log it to the console
        PR->>K: commit offset
    end
```

## When something goes wrong

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant OS as order-service
    participant K as Kafka topic pedidos
    participant PS as processing-service
    participant DB as MongoDB

    alt invalid fields
        Client->>OS: POST /orders (blank name, quantity 0...)
        OS-->>Client: 400 Bad Request + errors per field
    else business rule broken
        Client->>OS: POST /orders (same product twice)
        OS-->>Client: 422 Unprocessable Entity (InvalidOrderException)
    else Kafka unavailable
        Client->>OS: POST /orders
        OS-xK: send fails or times out
        OS-->>Client: 503 Service Unavailable (OrderPublishingException)
    end

    alt same order delivered twice
        K->>PS: OrderCreatedEvent (redelivery)
        PS->>DB: insert
        DB-->>PS: duplicate key
        PS->>PS: ignore, count orders.duplicates
    else MongoDB down
        K->>PS: OrderCreatedEvent
        loop up to 3 retries, 1s apart
            PS->>DB: insert
            DB--xPS: error
        end
        PS->>PS: log and skip the record
    end
```

Order publishing and order storage are decoupled. When the client gets `202`, the order is durably in Kafka. If a consumer is down at that moment, it catches up from its last committed offset when it comes back.
