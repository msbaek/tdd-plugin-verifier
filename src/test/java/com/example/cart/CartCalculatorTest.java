package com.example.cart;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartCalculatorTest {

    private final CartCalculator calculator = new CartCalculator();

    @Test
    void 빈_장바구니는_배송비도_붙지_않는다() {
        final long finalAmount = calculator.calculate(new CalculateCartRequest(List.of(), 0, 0));

        assertThat(finalAmount).isEqualTo(0);
    }

    @Test
    void 계산_요청_자체가_null이면_거부된다() {
        assertThatThrownBy(() -> calculator.calculate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 라인_목록_자체가_null이면_거부된다() {
        assertThatThrownBy(() -> calculator.calculate(new CalculateCartRequest(null, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 라인_목록_안에_null_라인이_섞여_있으면_거부된다() {
        final List<CartLine> lines = new java.util.ArrayList<>();
        lines.add(new CartLine("상품", 10_000L, 1));
        lines.add(null);

        assertThatThrownBy(() -> calculator.calculate(new CalculateCartRequest(lines, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
