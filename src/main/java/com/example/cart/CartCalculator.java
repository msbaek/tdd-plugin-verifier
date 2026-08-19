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

        final List<CartLine> lines = request.lines();
        final long productTotal = sumProductTotal(lines);
        final long shippingFee = shippingFeeFor(lines);

        // 쿠폰은 상품 합계에서만 차감하고, 0 미만으로 내려가지 않는다(§1).
        final long productBalanceAfterCoupon = Math.max(0, productTotal - request.coupon());

        // 마일리지는 상품 잔액을 먼저 차감하고, 남으면 배송비에 적용한다(§1).
        return deductMileage(productBalanceAfterCoupon, shippingFee, request.mileage());
    }

    /**
     * §1 검증 순서(불변식): null 층 전체(request → lines → 개별 line)가 항상 먼저이고,
     * 그다음 필드 층(라인 순서대로 수량 → 단가, 이어서 쿠폰 → 마일리지)이다.
     */
    private void validate(final CalculateCartRequest request) {
        if (request == null) {
            throw new NullPointerException("request must not be null");
        }
        final List<CartLine> lines = request.lines();
        if (lines == null) {
            throw new NullPointerException("lines must not be null");
        }
        validateLinesNotNull(lines);
        validateLineFields(lines);

        if (request.coupon() < 0) {
            throw new IllegalArgumentException("coupon must be >= 0");
        }
        if (request.mileage() < 0) {
            throw new IllegalArgumentException("mileage must be >= 0");
        }
    }

    private void validateLinesNotNull(final List<CartLine> lines) {
        for (final CartLine line : lines) {
            if (line == null) {
                throw new NullPointerException("line must not be null");
            }
        }
    }

    private void validateLineFields(final List<CartLine> lines) {
        for (final CartLine line : lines) {
            if (line.quantity() < 1) {
                throw new IllegalArgumentException("quantity must be >= 1");
            }
            if (line.unitPrice() < 0) {
                throw new IllegalArgumentException("unitPrice must be >= 0");
            }
        }
    }

    private long sumProductTotal(final List<CartLine> lines) {
        long productTotal = 0;
        for (final CartLine line : lines) {
            productTotal += line.unitPrice() * line.quantity();
        }
        return productTotal;
    }

    private long shippingFeeFor(final List<CartLine> lines) {
        return lines.isEmpty() ? 0L : SHIPPING_FEE;
    }

    private long deductMileage(final long productBalance, final long shippingFee, final long mileage) {
        long remainingProductBalance = productBalance;
        long remainingMileage = mileage;

        final long deductedFromProduct = Math.min(remainingProductBalance, remainingMileage);
        remainingProductBalance -= deductedFromProduct;
        remainingMileage -= deductedFromProduct;

        final long deductedFromShipping = Math.min(shippingFee, remainingMileage);
        final long remainingShippingBalance = shippingFee - deductedFromShipping;

        return remainingProductBalance + remainingShippingBalance;
    }
}
