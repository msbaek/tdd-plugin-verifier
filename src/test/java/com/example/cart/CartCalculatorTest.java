package com.example.cart;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
        final List<CartLine> lines = new ArrayList<>();
        lines.add(aLine(10_000L, 1));
        lines.add(null);

        assertThatThrownBy(() -> calculator.calculate(new CalculateCartRequest(lines, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void null_층이_필드_층보다_항상_먼저_검사된다() {
        assertThatThrownBy(() -> calculator.calculate(new CalculateCartRequest(null, -1, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("라인 목록");
    }

    @Test
    void 여러_필드가_동시에_유효하지_않으면_수량_위반이_우선한다() {
        final List<CartLine> lines = List.of(aLine(-1L, 0));

        assertThatThrownBy(() -> calculator.calculate(new CalculateCartRequest(lines, -1, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수량");
    }

    @Test
    void 서로_다른_라인의_서로_다른_필드가_위반해도_필드_우선순위가_라인_순서보다_우선한다() {
        // 라인 A(1번째): 단가 위반만. 라인 B(2번째): 수량 위반만.
        // §1: "필드 유효성 안에서는 수량→단가→…" 순서가 전체 라인에 걸쳐 먼저 적용된다 —
        // "라인 순서 우선"은 같은 필드를 검사하는 동안에만 적용되는 하위 규칙이다.
        // 따라서 라인 순서상 앞선 A의 단가 위반이 아니라, 필드 순서상 앞선 수량 위반(B)이 이긴다.
        final List<CartLine> lines = List.of(aLine(-1L, 5), aLine(100L, 0));

        assertThatThrownBy(() -> calculator.calculate(new CalculateCartRequest(lines, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수량");
    }

    @Test
    void 위반_유형마다_구분_가능한_메시지가_나온다() {
        final String requestNull = messageOf(() -> calculator.calculate(null));
        final String linesNull = messageOf(() -> calculator.calculate(new CalculateCartRequest(null, 0, 0)));
        final String lineNull = messageOf(() -> {
            final List<CartLine> lines = new ArrayList<>();
            lines.add(null);
            calculator.calculate(new CalculateCartRequest(lines, 0, 0));
        });
        final String quantity = messageOf(() -> calculator.calculate(
                new CalculateCartRequest(List.of(aLine(1L, 0)), 0, 0)));
        final String unitPrice = messageOf(() -> calculator.calculate(
                new CalculateCartRequest(List.of(aLine(-1L, 1)), 0, 0)));
        final String coupon = messageOf(() -> calculator.calculate(
                new CalculateCartRequest(List.of(), -1, 0)));
        final String mileage = messageOf(() -> calculator.calculate(
                new CalculateCartRequest(List.of(), 0, -1)));

        assertThat(Set.of(requestNull, linesNull, lineNull, quantity, unitPrice, coupon, mileage))
                .hasSize(7);
    }

    @Test
    void 무할인_기준선_상품합계와_배송비를_그대로_반환한다() {
        final List<CartLine> lines = List.of(aLine(10_000L, 1));

        final long finalAmount = calculator.calculate(new CalculateCartRequest(lines, 0, 0));

        assertThat(finalAmount).isEqualTo(13_000);
    }

    @Test
    void 유효한_임의_입력에_대해_최종_결제_금액은_항상_0_이상이다() {
        final Random random = new Random(20260815L);

        for (int i = 0; i < 500; i++) {
            final int lineCount = random.nextInt(6);
            final List<CartLine> lines = new ArrayList<>();
            for (int j = 0; j < lineCount; j++) {
                lines.add(aLine(random.nextInt(100_001), 1 + random.nextInt(10)));
            }
            final long coupon = random.nextInt(100_001);
            final long mileage = random.nextInt(100_001);

            final long finalAmount = calculator.calculate(new CalculateCartRequest(lines, coupon, mileage));

            assertThat(finalAmount).isGreaterThanOrEqualTo(0);
        }
    }

    /** Test Data Builder — 계산 로직이 쓰지 않는 상품명은 고정값으로 감춘다. */
    private CartLine aLine(final long unitPrice, final int quantity) {
        return new CartLine("상품", unitPrice, quantity);
    }

    private String messageOf(final Runnable action) {
        try {
            action.run();
            throw new AssertionError("예외가 발생하지 않았다");
        } catch (final IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
