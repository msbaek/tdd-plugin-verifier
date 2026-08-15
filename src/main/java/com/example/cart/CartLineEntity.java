package com.example.cart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 장바구니 라인의 영속 표현. 도메인 값 객체 {@link CartLine}과 의도적으로 분리한다 —
 * 계산 규칙(§1)은 영속성을 알지 못하는 순수 함수로 남는다.
 *
 * <p>수량·단가에 DB 제약을 걸지 않는다: §1의 유효성 검사(수량 &lt; 1, 단가 &lt; 0 거부)는
 * 계산기의 책임이며, 그 경계 케이스(E-9·E-10)를 DB가 먼저 막으면 계산기를 검증할 수 없다.
 */
@Entity
@Table(name = "cart_lines")
public class CartLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product", nullable = false)
    private String product;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected CartLineEntity() {
    }

    CartLineEntity(final Cart cart, final String product, final long unitPrice, final int quantity) {
        this.cart = cart;
        this.product = product;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public String getProduct() {
        return product;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    /** §5 가드 테스트의 실패 주입 지점 — 정상 경로에서는 호출되지 않는다. */
    void changeQuantity(final int quantity) {
        this.quantity = quantity;
    }
}
