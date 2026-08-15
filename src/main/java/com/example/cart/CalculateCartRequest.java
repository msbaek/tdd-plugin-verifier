package com.example.cart;

import java.util.List;

/**
 * 장바구니 결제 금액 계산 요청. {@code lines}가 null인 것과 빈 리스트인 것은 §1 도메인
 * 규칙상 의미가 다르다 — null은 거부 대상, 빈 리스트는 정상 입력(빈 장바구니)이다.
 */
public record CalculateCartRequest(List<CartLine> lines, long coupon, long mileage) {
}
