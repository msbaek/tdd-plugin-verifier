package com.example.cart;

import java.util.List;

/**
 * 장바구니 결제 금액 계산기. §1 도메인 규칙(상품 합계 → 쿠폰 → 마일리지 → 배송비 합산)의
 * 순수 함수 구현체.
 */
public class CartCalculator {

    private static final long SHIPPING_FEE = 3_000L;

    public long calculate(final CalculateCartRequest request) {
        validate(request);
        if (request.lines().isEmpty()) {
            return 0;
        }
        final long productTotal = sumProductTotal(request.lines());
        final long afterCoupon = Math.max(0, productTotal - request.coupon());
        final long productBalance = Math.max(0, afterCoupon - request.mileage());
        final long mileageSpentOnProduct = afterCoupon - productBalance;
        final long mileageRemaining = request.mileage() - mileageSpentOnProduct;
        final long shippingBalance = Math.max(0, SHIPPING_FEE - mileageRemaining);
        return productBalance + shippingBalance;
    }

    private long sumProductTotal(final List<CartLine> lines) {
        long total = 0;
        for (final CartLine line : lines) {
            total += line.unitPrice() * line.quantity();
        }
        return total;
    }

    /** §1 검사 순서 정본: 요청 null → 라인목록 null → 개별라인 null → 수량 → 단가 → 쿠폰 → 마일리지. */
    private void validate(final CalculateCartRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("계산 요청은 null일 수 없다");
        }
        if (request.lines() == null) {
            throw new IllegalArgumentException("라인 목록은 null일 수 없다");
        }
        for (final CartLine line : request.lines()) {
            if (line == null) {
                throw new IllegalArgumentException("라인은 null일 수 없다");
            }
        }
        for (final CartLine line : request.lines()) {
            if (line.quantity() < 1) {
                throw new IllegalArgumentException("수량은 1 이상이어야 한다");
            }
        }
        for (final CartLine line : request.lines()) {
            if (line.unitPrice() < 0) {
                throw new IllegalArgumentException("단가는 음수일 수 없다");
            }
        }
        if (request.coupon() < 0) {
            throw new IllegalArgumentException("쿠폰은 음수일 수 없다");
        }
        if (request.mileage() < 0) {
            throw new IllegalArgumentException("마일리지는 음수일 수 없다");
        }
    }
}
