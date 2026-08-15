package com.example.cart;

/**
 * 결제 금액 계산 REST 요청 본문. 라인은 본문에 없다 — 장바구니 ID로 DB에서 조회한다
 * (Cart.md §1 "인수 테스트 채널 결정").
 */
public record CheckoutRequest(long coupon, long mileage) {
}
