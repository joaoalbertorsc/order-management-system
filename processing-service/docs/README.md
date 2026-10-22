# processing-service

Consumes every order published on the Kafka topic `pedidos` and stores it in MongoDB. It owns the order data, so it also serves the read side of the system: listing orders, showing one order and changing its status.

- **Port:** 8082
- **Consumes:** `pedidos`, consumer group `processing-service`
- **Stores in:** MongoDB, database `order_management`, collection `orders`
- **Depends on:** Kafka, MongoDB

Diagrams:

- [Components](diagrams/components.md): classes and both entry points (Kafka and REST)
- [Consume order sequence](diagrams/consume-order-sequence.md): storage, duplicates, invalid messages and retries
- [Order lifecycle](diagrams/order-lifecycle.md): statuses and the allowed transitions

## Kafka consumer

`OrderCreatedListener` receives each `OrderCreatedEvent` (see the [event contract](../../docs/architecture.md#event-contract-ordercreatedevent)) and calls `OrderProcessingService`. That service:

1. Validates the event (`OrderEventValidator`, Bean Validation constraints on the event record).
2. Inserts the order with status `CREATED`, using the order id as the MongoDB `_id`.
3. Ignores the message if that `_id` already exists. Kafka delivers *at least once*, so the same order may arrive twice, and storing it is idempotent.

| Problem | Behavior |
|---|---|
| Message is not valid JSON | `ErrorHandlingDeserializer` turns it into an error. It is logged and skipped without retrying. |
| Event misses required data | `InvalidOrderEventException`. Logged and skipped without retrying. |
| Same order received again | `DuplicateKeyException` is caught. Logged as a warning, metric `orders.duplicates`. |
| MongoDB unavailable or another transient error | Retried 3 times, 1 second apart (`DefaultErrorHandler`), then logged and skipped. |

The offset is committed only after a record is handled, so a crash in the middle of processing redelivers the record instead of losing it.

## REST API

### `GET /orders`

Lists stored orders, newest first.

| Query parameter | Default | Description |
|---|---|---|
| `status` | (all) | `CREATED`, `CONFIRMED`, `SHIPPED`, `DELIVERED` or `CANCELLED` |
| `page` | `0` | Page number, zero-based |
| `size` | `20` | Page size, at most 100 |
| `sort` | `createdAt,desc` | Any order field, e.g. `totalAmount,asc` |

```json
{
  "content": [
    {
      "id": "3ef482fc-d104-4710-9c34-6a8b440ac13f",
      "customerName": "Ana Souza",
      "customerEmail": "ana@example.com",
      "items": [
        { "productId": "SKU-1", "productName": "Mechanical keyboard", "quantity": 2, "unitPrice": 150.00, "subtotal": 300.00 }
      ],
      "totalAmount": 300.00,
      "status": "CREATED",
      "createdAt": "2026-10-21T17:51:43.946Z",
      "updatedAt": "2026-10-21T17:51:43.946Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### `GET /orders/{id}`

Returns one order (same shape as the items in `content` above), or `404` if it doesn't exist.

### `PATCH /orders/{id}/status`

```json
{ "status": "CONFIRMED" }
```

Moves the order to a new status, following the [lifecycle](diagrams/order-lifecycle.md). Returns the updated order.

### Errors

| Status | When | Exception |
|---|---|---|
| `400` | `status` missing, unknown status value (the response lists the accepted ones), or malformed JSON | Bean Validation / `HttpMessageNotReadableException` |
| `404` | No order with that id | `OrderNotFoundException` |
| `409` | Transition not allowed. The response includes `currentStatus` and `allowedStatuses`. | `InvalidStatusTransitionException` |
| `409` | The order was changed by another request at the same moment | `OptimisticLockingFailureException` |
| `500` | Anything unexpected (details are only logged) | any other exception |

Example `409`:

```json
{
  "type": "about:blank",
  "title": "Invalid status transition",
  "status": 409,
  "detail": "Order 3ef482fc-... cannot change from CONFIRMED to CREATED",
  "instance": "/orders/3ef482fc-.../status",
  "currentStatus": "CONFIRMED",
  "allowedStatuses": ["SHIPPED", "CANCELLED"]
}
```

## MongoDB document

```javascript
{
  _id: "3ef482fc-d104-4710-9c34-6a8b440ac13f",   // order id from the order-service
  customerName: "Ana Souza",
  customerEmail: "ana@example.com",
  items: [
    { productId: "SKU-1", productName: "Mechanical keyboard", quantity: 2, unitPrice: Decimal128("150.00") }
  ],
  totalAmount: Decimal128("300.00"),   // exact decimal, no floating-point rounding
  status: "CREATED",
  createdAt: ISODate("2026-10-21T17:51:43.946Z"),
  updatedAt: ISODate("2026-10-21T17:51:43.946Z"),
  version: 0,                          // optimistic locking
  _class: "com.ordermanagement.processing.domain.Order"
}
```

Indexes: `_id` (unique, which is what detects duplicates), `{ status: 1, createdAt: -1 }` for the filtered listing and `createdAt` for the unfiltered one.

## Configuration

| Property | Default | Description |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` (env) | `localhost:9092` | Kafka address |
| `MONGODB_URI` (env) | `mongodb://localhost:27017/order_management` | MongoDB connection |
| `app.kafka.orders-topic` | `pedidos` | Topic to consume |
| `spring.kafka.consumer.group-id` | `processing-service` | Consumer group |
| `spring.kafka.consumer.auto-offset-reset` | `earliest` | A new group reads the topic from the beginning |

## Observability

| Endpoint | Notes |
|---|---|
| `/actuator/health` | Includes `kafka` (custom) and `mongo` (Spring Boot) |
| `/actuator/info` | Name, description, build version and time, Java, OS |
| `/actuator/metrics/orders.stored` | Orders inserted into MongoDB |
| `/actuator/metrics/orders.duplicates` | Redelivered orders that were ignored |

## Code structure

```
com.ordermanagement.processing
├── api/                    OrderController, DTOs (OrderResponse, PageResponse, UpdateOrderStatusRequest), GlobalExceptionHandler
├── config/                 Kafka consumer factory and error handler, Clock
├── domain/                 Order (MongoDB document, enforces the lifecycle), OrderItem, OrderStatus
├── exception/              OrderNotFoundException, InvalidStatusTransitionException, InvalidOrderEventException
├── health/                 KafkaHealthIndicator
├── messaging/              OrderCreatedListener, OrderCreatedEvent, OrderEventValidator
├── repository/             OrderRepository (Spring Data MongoDB)
└── service/                OrderProcessingService (stores events), OrderService (queries and status changes)
```
