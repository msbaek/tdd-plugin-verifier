package com.example.cart;

import java.util.List;

/**
 * 장바구니 결제 금액 계산기. §1 도메인 규칙(상품 합계 → 쿠폰 → 마일리지 → 배송비 합산)의
 * 순수 함수 구현체.
 */
public class CartCalculator {

    private static final long SHIPPING_FEE = 3_000L;

    public long calculate(final CalculateCartRequest request) {
        if (request == null) {
            throw new NullPointerException("request must not be null");
        }
        final List<CartLine> lines = request.lines();
        if (lines == null) {
            throw new NullPointerException("lines must not be null");
        }
        for (final CartLine line : lines) {
            if (line == null) {
                throw new NullPointerException("line must not be null");
            }
        }
        for (final CartLine line : lines) {
            if (line.quantity() < 1) {
                throw new IllegalArgumentException("quantity must be >= 1");
            }
            if (line.unitPrice() < 0) {
                throw new IllegalArgumentException("unitPrice must be >= 0");
            }
        }
        if (request.coupon() < 0) {
            throw new IllegalArgumentException("coupon must be >= 0");
        }
        if (request.mileage() < 0) {
            throw new IllegalArgumentException("mileage must be >= 0");
        }

        long productTotal = 0;
        for (final CartLine line : lines) {
            productTotal += line.unitPrice() * line.quantity();
        }

        final long shippingFee = lines.isEmpty() ? 0L : SHIPPING_FEE;

        long productBalance = productTotal - request.coupon();
        if (productBalance < 0) {
            productBalance = 0;
        }

        long remainingMileage = request.mileage();
        final long deductedFromProduct = Math.min(productBalance, remainingMileage);
        productBalance -= deductedFromProduct;
        remainingMileage -= deductedFromProduct;

        long shippingBalance = shippingFee;
        final long deductedFromShipping = Math.min(shippingBalance, remainingMileage);
        shippingBalance -= deductedFromShipping;

        return productBalance + shippingBalance;
    }
}
