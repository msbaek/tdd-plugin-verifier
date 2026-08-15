package com.example.cart;

/**
 * 장바구니 한 라인 — (상품, 단가, 수량). 값 객체.
 * §5(Walking Skeleton)·§6(RGB)에서 계산 로직이 이 타입을 소비한다.
 */
public record CartLine(String product, long unitPrice, int quantity) {
}
