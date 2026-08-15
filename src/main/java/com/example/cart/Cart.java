package com.example.cart;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * 장바구니 aggregate root. §5 Walking Skeleton의 최소 스키마 — 식별자와 라인 목록만 가진다.
 * 라인은 LAZY로 유지하고, 조회 지점({@link CartRepository#findWithLinesById})에서
 * {@code @EntityGraph}로 명시적으로 당긴다(전역 EAGER 금지).
 */
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 조회 결과를 삽입 순서(id 오름차순)로 결정적으로 고정한다. 단, 현재 계산 로직은
     * 상품 합계(교환법칙 성립)·입력 검증(전 라인 스캔, 라인 순서 무관) 어느 쪽도 라인
     * 순서에 실제로 의존하지 않는다 — 방어적 결정성일 뿐 도메인 계약이 아니다
     * (mid 적대적 리뷰 MAJOR #2: E-16이 이 순서를 검증한다는 이전 주장은 부정확했다 —
     * E-16이 실제로 잡는 것은 "첫 라인만 검사하는 얕은 구현"이지 순서 자체가 아니다).
     */
    @OneToMany(mappedBy = "cart", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CartLineEntity> lines = new ArrayList<>();

    protected Cart() {
    }

    public static Cart empty() {
        return new Cart();
    }

    public void addLine(final String product, final long unitPrice, final int quantity) {
        lines.add(new CartLineEntity(this, product, unitPrice, quantity));
    }

    public Long getId() {
        return id;
    }

    public List<CartLineEntity> getLines() {
        return lines;
    }
}
