# Consume order sequence (processing-service)

```mermaid
sequenceDiagram
    autonumber
    participant K as Kafka topic pedidos
    participant D as ErrorHandlingDeserializer
    participant L as OrderCreatedListener
    participant S as OrderProcessingService
    participant V as OrderEventValidator
    participant DB as MongoDB
    participant E as DefaultErrorHandler

    K->>D: record (key = orderId, JSON value)
    alt not valid JSON
        D-->>E: DeserializationException
        E->>K: original bytes to the dead letter topic (no retry)
        E->>K: commit offset
    else valid JSON
        D->>L: OrderCreatedEvent
        L->>S: process(event)
        S->>V: validate(event)
        alt required data missing
            V-->>E: InvalidOrderEventException
            E->>K: event to the dead letter topic (no retry)
            E->>K: commit offset
        else event valid
            S->>DB: insert Order (status CREATED, _id = orderId)
            alt inserted
                DB-->>S: ok
                S->>S: orders.stored + 1
            else _id already exists
                DB-->>S: duplicate key
                S->>S: ignore, orders.duplicates + 1
            else MongoDB unavailable
                DB--xS: error
                S-->>E: exception
                E->>L: redeliver with exponential backoff (1 s, 2 s, 4 s... max 30 s), for up to 5 min
                E->>K: still failing, so send it to the dead letter topic
            end
            L->>K: commit offset
        end
    end
```

The offset is committed only after a record is handled (stored, ignored as duplicate, or moved to the dead letter topic). If the service crashes halfway, Kafka redelivers the record on restart, and the duplicate check keeps that redelivery harmless.
