package com.ordermanagement.order.domain;

import java.math.BigDecimal;

public record OrderItem(String productId, String productName, int quantity, BigDecimal unitPrice) {

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
