# print-service

The second consumer of the `pedidos` topic. It prints every order it receives to the console as a receipt. It stores nothing and exposes no business API, only the Actuator endpoints.

- **Port:** 8083 (Actuator only)
- **Consumes:** `pedidos`, consumer group `print-service`
- **Depends on:** Kafka

Diagrams:

- [Components](diagrams/components.md)
- [Print order sequence](diagrams/print-order-sequence.md)

## Output

Each order shows up in the console like this (`docker compose logs -f print-service`):

```
2026-10-21T17:51:44.229Z  INFO 1 --- [print-service] [ntainer#0-0-C-1] c.o.p.messaging.OrderCreatedListener     : Received order 3ef482fc-d104-4710-9c34-6a8b440ac13f (partition=1, offset=0)
2026-10-21T17:51:44.231Z  INFO 1 --- [print-service] [ntainer#0-0-C-1] c.o.print.printer.ConsoleOrderPrinter    : Processing order 3ef482fc-d104-4710-9c34-6a8b440ac13f
============================================================
  NEW ORDER RECEIVED
============================================================
  Order      : 3ef482fc-d104-4710-9c34-6a8b440ac13f
  Customer   : Ana Souza <ana@example.com>
  Created at : 2026-10-21T17:51:43.946Z
  Items      :
    2 x Mechanical keyboard (SKU-1) @ 150.00 = 300.00
    1 x Mouse (SKU-2) @ 79.90 = 79.90
  Total      : 379.90
============================================================
```

The receipt goes through the application logger rather than `System.out`, so it carries a timestamp and the Kafka partition and offset it came from. It also ends up wherever the logs are collected.

## How it works

- Its consumer group is `print-service`, different from the processing-service's. Kafka therefore delivers **every** order to both services, independently.
- `OrderCreatedListener` receives the event and calls `OrderPrinter`, an interface. `ConsoleOrderPrinter` implements it, and `OrderReceiptFormatter` builds the text.
- The formatter is tolerant: a missing field is printed as `-`, so an incomplete message is still printed instead of failing.

| Problem | Behavior |
|---|---|
| Message is not valid JSON | Logged and skipped without retrying |
| Any other error while printing | Retried twice, 1 second apart, then logged and skipped, so one bad record never blocks the rest |

## Configuration

| Property | Default | Description |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` (env) | `localhost:9092` | Kafka address |
| `app.kafka.orders-topic` | `pedidos` | Topic to consume |
| `spring.kafka.consumer.group-id` | `print-service` | Consumer group |
| `spring.kafka.consumer.auto-offset-reset` | `earliest` | A new group reads the topic from the beginning |

## Observability

| Endpoint | Notes |
|---|---|
| `/actuator/health` | Includes `kafka` (custom `KafkaHealthIndicator`) |
| `/actuator/info` | Name, description, build version and time, Java, OS |
| `/actuator/metrics/orders.printed` | Orders printed |

## Code structure

```
com.ordermanagement.print
├── config/                 Kafka consumer factory and error handler
├── health/                 KafkaHealthIndicator
├── messaging/              OrderCreatedListener, OrderCreatedEvent
└── printer/                OrderPrinter (interface), ConsoleOrderPrinter, OrderReceiptFormatter
```
