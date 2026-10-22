# Query and status update flow

Once stored, orders are read and moved through their lifecycle on the processing-service.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant PS as processing-service
    participant DB as MongoDB

    Client->>PS: GET /orders?status=CREATED&page=0&size=20
    PS->>DB: find by status, sorted by createdAt desc
    DB-->>PS: page of orders
    PS-->>Client: 200 OK + content, page, size, totalElements, totalPages

    Client->>PS: GET /orders/{id}
    PS->>DB: findById
    alt found
        PS-->>Client: 200 OK + order
    else not found
        PS-->>Client: 404 Not Found
    end

    Client->>PS: PATCH /orders/{id}/status {"status": "CONFIRMED"}
    PS->>DB: findById
    PS->>PS: order.changeStatus(CONFIRMED) checks the lifecycle
    alt transition allowed
        PS->>DB: save (optimistic lock on version)
        PS-->>Client: 200 OK + updated order
    else transition not allowed
        PS-->>Client: 409 Conflict + allowedStatuses
    else updated concurrently by another request
        PS-->>Client: 409 Conflict, reload and retry
    end
```

The allowed transitions are in the [order lifecycle diagram](../../processing-service/docs/diagrams/order-lifecycle.md).
