# print-service components

```mermaid
flowchart LR
    kafka[["Kafka topic pedidos"]]
    console[/Console/]

    subgraph messaging[messaging]
        listener[OrderCreatedListener]
        event[OrderCreatedEvent]
    end

    subgraph printer[printer]
        printerIf{{"OrderPrinter<br/>(interface)"}}
        consolePrinter[ConsoleOrderPrinter]
        formatter[OrderReceiptFormatter]
    end

    errorHandler["DefaultErrorHandler<br/>retry 2x, then skip"]

    kafka -- "consumer group print-service" --> listener
    listener -- "deserialized" --> event
    listener --> printerIf
    consolePrinter -. implements .-> printerIf
    consolePrinter -- "format(order)" --> formatter
    consolePrinter -- "log.info(receipt)" --> console
    listener -. "exceptions" .-> errorHandler
```

The listener only knows the `OrderPrinter` interface. Printing to a file, to a thermal printer or to a chat channel would need a new implementation, and the Kafka side wouldn't change. Formatting is a separate class, so it can be unit-tested without capturing console output.
