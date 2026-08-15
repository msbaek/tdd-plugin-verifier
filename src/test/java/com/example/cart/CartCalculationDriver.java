package com.example.cart;

import java.util.ArrayList;
import java.util.List;

/**
 * Protocol Driver — Cucumber Steps와 SUT({@link CartCalculator}) 사이의 유일한 상호작용
 * 지점. in-process 호출이므로 빠르다. 채널이 HTTP 등으로 바뀌어도 Steps는 이 클래스의
 * 공개 메서드 시그니처만 그대로면 바뀔 필요가 없다.
 */
class CartCalculationDriver {

    private static final String DEFAULT_PRODUCT_NAME = "상품";

    private final CartCalculator calculator = new CartCalculator();

    private List<CartLine> lines = new ArrayList<>();
    private boolean linesNull = false;
    private long coupon = 0;
    private long mileage = 0;

    private Long finalAmount;
    private boolean rejected;

    void addLine(final long unitPrice, final int quantity) {
        lines.add(new CartLine(DEFAULT_PRODUCT_NAME, unitPrice, quantity));
    }

    void addNullLine() {
        lines.add(null);
    }

    void makeLinesNull() {
        this.linesNull = true;
    }

    void setCoupon(final long coupon) {
        this.coupon = coupon;
    }

    void setMileage(final long mileage) {
        this.mileage = mileage;
    }

    /** E-9~E-12: 요청의 한 필드(수량/단가/쿠폰/마일리지)를 지정된 값으로 덮어쓴다. */
    void overwriteField(final String field, final long value) {
        switch (field) {
            case "수량" -> overwriteLastLineQuantity((int) value);
            case "단가" -> overwriteLastLineUnitPrice(value);
            case "쿠폰" -> this.coupon = value;
            case "마일리지" -> this.mileage = value;
            default -> throw new IllegalArgumentException("알 수 없는 필드: " + field);
        }
    }

    /** E-16: n번째(1-based) 라인의 수량만 덮어쓴다. */
    void overwriteLineQuantity(final int oneBasedIndex, final int quantity) {
        final int index = oneBasedIndex - 1;
        final CartLine old = lines.get(index);
        lines.set(index, new CartLine(old.product(), old.unitPrice(), quantity));
    }

    void calculate() {
        final List<CartLine> requestLines = linesNull ? null : lines;
        final CalculateCartRequest request = new CalculateCartRequest(requestLines, coupon, mileage);
        try {
            finalAmount = calculator.calculate(request);
            rejected = false;
        } catch (final RuntimeException e) {
            finalAmount = null;
            rejected = true;
        }
    }

    long finalAmount() {
        return finalAmount;
    }

    boolean wasRejected() {
        return rejected;
    }

    private void overwriteLastLineQuantity(final int quantity) {
        final int lastIndex = lines.size() - 1;
        final CartLine old = lines.get(lastIndex);
        lines.set(lastIndex, new CartLine(old.product(), old.unitPrice(), quantity));
    }

    private void overwriteLastLineUnitPrice(final long unitPrice) {
        final int lastIndex = lines.size() - 1;
        final CartLine old = lines.get(lastIndex);
        lines.set(lastIndex, new CartLine(old.product(), unitPrice, old.quantity()));
    }
}
