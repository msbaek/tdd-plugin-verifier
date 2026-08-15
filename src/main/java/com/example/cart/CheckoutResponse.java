package com.example.cart;

/** 결제 금액 계산 REST 응답 본문. 엔티티가 아니라 DTO를 반환한다(§5 영속성 경계 결정). */
public record CheckoutResponse(long finalAmount) {
}
