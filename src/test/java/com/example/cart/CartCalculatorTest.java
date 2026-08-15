package com.example.cart;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartCalculatorTest {

    private final CartCalculator calculator = new CartCalculator();

    @Test
    void 빈_장바구니는_배송비도_붙지_않는다() {
        final long finalAmount = calculator.calculate(new CalculateCartRequest(List.of(), 0, 0));

        assertThat(finalAmount).isEqualTo(0);
    }
}
