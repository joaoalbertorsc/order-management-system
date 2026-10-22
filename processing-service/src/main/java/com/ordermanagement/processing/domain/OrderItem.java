package com.ordermanagement.processing.domain;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

public record OrderItem(
        String productId,
        String productName,
        int quantity,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal unitPrice) {

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
