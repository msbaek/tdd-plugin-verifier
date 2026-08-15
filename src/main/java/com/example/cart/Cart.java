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

    /** §1 검사 순서 정본의 "라인 순서"는 이 컬럼(id 오름차순 = 삽입 순서)이 정의한다(E-16). */
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
