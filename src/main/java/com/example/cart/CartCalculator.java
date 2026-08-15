package com.example.cart;

/**
 * 장바구니 결제 금액 계산기. §1 도메인 규칙(상품 합계 → 쿠폰 → 마일리지 → 배송비 합산)의
 * 순수 함수 구현체. §5 Walking Skeleton·§6 RGB 사이클에서 채워진다 — 이 단계(§3 인수 테스트
 * 셋업)에서는 시그니처만 확정하고 실제 계산 로직은 구현하지 않는다.
 */
public class CartCalculator {

    public long calculate(final CalculateCartRequest request) {
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
        return 0;
    }
}
