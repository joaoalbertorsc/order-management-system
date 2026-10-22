# order-service

Entry point for new orders. It validates each order, gives it an id and publishes it to the Kafka topic `pedidos`. It stores nothing itself; the [processing-service](../../processing-service/docs/README.md) stores the orders.

- **Port:** 8081
- **Publishes to:** `pedidos` (creates the topic on startup if missing)
- **Depends on:** Kafka

Diagrams:

- [Components](diagrams/components.md): classes and how a request flows through them
- [Create order sequence](diagrams/create-order-sequence.md): happy path and every error path

## REST API

### `POST /orders`

Request:

```json
{
  "customerName": "Ana Souza",
  "customerEmail": "ana@example.com",
  "items": [
    { "productId": "SKU-1", "productName": "Mechanical keyboard", "quantity": 2, "unitPrice": 150.00 },
    { "productId": "SKU-2", "productName": "Mouse", "quantity": 1, "unitPrice": 79.90 }
  ]
}
```

| Field | Rules |
|---|---|
| `customerName` | required, up to 120 characters |
| `customerEmail` | required, valid e-mail |
| `items` | 1 to 50 items |
| `items[].productId` | required, up to 64 characters, unique within the order |
| `items[].productName` | required, up to 120 characters |
| `items[].quantity` | 1 to 1000 |
| `items[].unitPrice` | at least 0.01, up to 2 decimal places |

Response `202 Accepted`:

```json
{
  "id": "3ef482fc-d104-4710-9c34-6a8b440ac13f",
  "customerName": "Ana Souza",
  "customerEmail": "ana@example.com",
  "items": [
    { "productId": "SKU-1", "productName": "Mechanical keyboard", "quantity": 2, "unitPrice": 150.00, "subtotal": 300.00 },
    { "productId": "SKU-2", "productName": "Mouse", "quantity": 1, "unitPrice": 79.90, "subtotal": 79.90 }
  ],
  "totalAmount": 379.90,
  "createdAt": "2026-10-21T17:51:43.946Z"
}
```

Why `202` and not `201`: the order is safely in Kafka, but it is stored by another service a few milliseconds later. Use the returned `id` to read it from the processing-service (`GET :8082/orders/{id}`).

### Errors

| Status | When | Exception |
|---|---|---|
| `400` | A field breaks the rules above, or the JSON is malformed. The body lists every invalid field. | `MethodArgumentNotValidException` / `HttpMessageNotReadableException` |
| `422` | A business rule rejects the order | `InvalidOrderException` |
| `503` | Kafka did not acknowledge the order (broker down or timeout, about 10 s at most) | `OrderPublishingException` |
| `500` | Anything unexpected (details are only logged) | any other exception |

Example `400`:

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/orders",
  "errors": {
    "customerName": "must not be blank",
    "customerEmail": "must be a well-formed email address",
    "items[0].quantity": "must be greater than 0"
  }
}
```

## Business rules

Each rule is a separate `OrderValidationRule` bean. The `OrderService` runs every rule it finds, so adding a rule never changes existing code.

| Rule | Rejects |
|---|---|
| `UniqueProductRule` | The same `productId` twice in one order (send one item with the total quantity) |
| `MaxTotalAmountRule` | Orders whose total exceeds `app.orders.max-total-amount` (default 50,000.00) |

## Kafka producer

| Setting | Value | Why |
|---|---|---|
| Key | order id | Keeps all messages of one order on the same partition |
| Value | `OrderCreatedEvent` as JSON, no type headers | See the [event contract](../../docs/architecture.md#event-contract-ordercreatedevent) |
| `acks` | `all` | The broker confirms only after the record is safely written |
| `enable.idempotence` | `true` | Producer retries cannot create duplicates |
| `max.block.ms` / `request.timeout.ms` | 5 s | Fail fast when the broker is unreachable |
| `delivery.timeout.ms` | 10 s | Upper bound for one send, retries included |

`KafkaOrderEventPublisher` waits for the broker's acknowledgment before the HTTP response is sent. If none arrives, it throws `OrderPublishingException`, and the client gets a `503` instead of a false success.

## Configuration

| Property | Default | Description |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` (env) | `localhost:9092` | Kafka address |
| `app.kafka.orders-topic.name` | `pedidos` | Topic name |
| `app.kafka.orders-topic.partitions` | `3` | Partitions when the topic is created |
| `app.kafka.orders-topic.replication-factor` | `1` | Replicas when the topic is created |
| `app.kafka.send-timeout` | `15s` | Maximum time the request waits for Kafka |
| `app.orders.max-total-amount` | `50000.00` | Highest accepted order total |

## Observability

| Endpoint | Notes |
|---|---|
| `/actuator/health` | Includes `kafka` (custom `KafkaHealthIndicator`: cluster id and node count) |
| `/actuator/info` | Name, description, build version and time, Java, OS |
| `/actuator/metrics/orders.published` | Orders acknowledged by Kafka |
| `/actuator/metrics/orders.publish.failures` | Orders rejected with `503` |

## Code structure

```
com.ordermanagement.order
├── api/                    OrderController, request/response DTOs, GlobalExceptionHandler
├── config/                 Kafka producer and topic, typed properties, Clock
├── domain/                 Order, OrderItem, Customer (immutable records)
├── exception/              InvalidOrderException, OrderPublishingException
├── health/                 KafkaHealthIndicator
├── messaging/              OrderEventPublisher (port), KafkaOrderEventPublisher, OrderCreatedEvent
├── service/                OrderService (use case: create order)
└── validation/             OrderValidationRule and its implementations
```
