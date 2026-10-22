# processing-service components

The service has two entry points. The **Kafka listener** writes new orders, and the **REST controller** reads them and changes their status. Each entry point has its own service class, and both use the same repository.

```mermaid
flowchart LR
    kafka[["Kafka topic pedidos"]]
    client([Client])
    mongo[("MongoDB<br/>orders")]

    subgraph messaging[messaging]
        listener[OrderCreatedListener]
        validator[OrderEventValidator]
        event[OrderCreatedEvent]
    end

    subgraph api[api]
        controller[OrderController]
        handler[GlobalExceptionHandler]
    end

    subgraph service[service]
        processing[OrderProcessingService]
        orderService[OrderService]
    end

    subgraph domain[domain]
        order["Order<br/>changeStatus() enforces the lifecycle"]
        status[OrderStatus]
    end

    repo[OrderRepository]
    errorHandler["DefaultErrorHandler<br/>retry 3x, then skip"]

    kafka -- "JSON" --> listener
    listener -- "deserialized" --> event
    listener --> processing
    processing --> validator
    processing -- "insert" --> repo
    listener -. "exceptions" .-> errorHandler

    client -- "GET / PATCH" --> controller
    controller --> orderService
    orderService -- "find / save" --> repo
    orderService -- "changeStatus" --> order
    order --> status
    controller -. "exceptions" .-> handler
    handler -- "400 / 404 / 409 / 500" --> client

    repo --> mongo
```

| Class | Responsibility |
|---|---|
| `OrderCreatedListener` | Receives Kafka records and hands them to the service |
| `OrderProcessingService` | Validates an event and stores it, ignoring duplicates |
| `OrderService` | Lists orders, finds one, changes its status |
| `Order` | Holds the data and refuses status changes the lifecycle doesn't allow |
| `OrderRepository` | MongoDB access (Spring Data) |
| `DefaultErrorHandler` | Retries transient failures and skips messages that can never be processed |
