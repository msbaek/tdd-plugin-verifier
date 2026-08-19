package com.example.cart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * 정본: Cart.md §1(입력 유효성 — null 3종)·§4-2(U-1, U-4, U-8, U-9)·§4-3(통합 RGB 순서).
 * §5 결정에 따라 E-14(라인 목록 null)·E-15(null 라인)도 여기서 검증한다
 * (REST 경로로 도달 불가 — Controller가 요청을 항상 직접 조립하므로).
 */
class CartCalculatorTest {

    private final CartCalculator calculator = new CartCalculator();

    @DisplayName("U-1: 계산 요청 자체가 null이면 NullPointerException으로 거부된다")
    @Test
    void rejects_when_request_itself_is_null() {
        assertThatNullPointerException()
                .isThrownBy(() -> calculator.calculate(null));
    }

    @DisplayName("E-14: 라인 목록이 null이면 NullPointerException으로 거부된다 (REST 채널 표현 불가로 이관)")
    @Test
    void rejects_when_lines_is_null() {
        final CalculateCartRequest request = new CalculateCartRequest(null, 0, 0);

        assertThatNullPointerException()
                .isThrownBy(() -> calculator.calculate(request));
    }

    @DisplayName("E-15: 라인 목록 안에 null 라인이 섞여 있으면 NullPointerException으로 거부된다 (REST 채널 표현 불가로 이관)")
    @Test
    void rejects_when_a_line_in_the_list_is_null() {
        final List<CartLine> lines = Arrays.asList(new CartLine("상품", 12_000L, 2), null);
        final CalculateCartRequest request = new CalculateCartRequest(lines, 0, 0);

        assertThatNullPointerException()
                .isThrownBy(() -> calculator.calculate(request));
    }

    // U-9: null 층이 필드 층보다 항상 먼저 검사된다(§1 검사 순서 정본의 층간 우선순위).
    // 적대적 리뷰(뮤테이션 테스트)가 지적한 대로, "예외가 던져진다"만 보면 필드 층 예외
    // (IllegalArgumentException)도 통과해 버려 순서 위반을 잡아내지 못한다. 구체 타입·메시지로
    // null 층(NullPointerException, "lines must not be null")이 실제로 이겼는지 확인한다.
    @DisplayName("U-9: null 층이 필드 층보다 항상 먼저 검사된다(라인 목록 null + 쿠폰 음수 동시 위반)")
    @Test
    void null_layer_is_checked_before_field_layer() {
        final CalculateCartRequest request = new CalculateCartRequest(null, -1, 0);

        assertThatThrownBy(() -> calculator.calculate(request))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("lines must not be null");
    }

    // U-4: 여러 필드가 동시에 유효하지 않을 때 우선순위대로 하나의 예외만 던진다(수량 → 단가
    // → 쿠폰 → 마일리지 순). 적대적 리뷰 지적대로, "예외가 던져진다"만 보면 어느 필드가
    // 이겼는지 구분하지 못한다. 수량<1(먼저)과 단가<0(나중)이 같은 라인에서 동시에 위반될 때
    // 수량 위반의 구체 메시지로 실제 우선순위가 지켜졌는지 확인한다.
    @DisplayName("U-4: 수량<1 AND 단가<0처럼 여러 필드가 동시에 유효하지 않을 때 수량 위반이 먼저 걸린다")
    @Test
    void rejects_when_multiple_fields_are_invalid_at_once() {
        final List<CartLine> lines = List.of(new CartLine("상품", -1L, 0));
        final CalculateCartRequest request = new CalculateCartRequest(lines, 0, 0);

        assertThatThrownBy(() -> calculator.calculate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be >= 1");
    }

    // U-8: §1 검사 순서 정본의 7가지 위반 유형(① 요청 자체 null ② 라인 목록 null ③ 개별
    // 라인 null ④ 수량<1 ⑤ 단가<0 ⑥ 쿠폰<0 ⑦ 마일리지<0) 각각에서 발생하는 예외가 서로
    // 구분 가능해야 한다(타입 또는 메시지). "구분 가능"의 최소 조작적 정의로, 각 케이스의
    // (예외 타입, 메시지) 조합을 모아 서로 다른 조합의 개수가 케이스 수(7)와 같은지 확인한다.
    @DisplayName("U-8: 위반 유형마다 구분 가능한 예외/메시지가 나온다")
    @Test
    void distinguishes_exception_by_violation_type() {
        final CartLine validLine = new CartLine("상품", 10_000L, 1);
        final List<CalculateCartRequest> violatingRequests = List.of(
                new CalculateCartRequest(List.of(new CartLine("상품", 10_000L, 0)), 0, 0),  // ④ 수량<1
                new CalculateCartRequest(List.of(new CartLine("상품", -1L, 1)), 0, 0),        // ⑤ 단가<0
                new CalculateCartRequest(List.of(validLine), -1, 0),                          // ⑥ 쿠폰<0
                new CalculateCartRequest(List.of(validLine), 0, -1)                           // ⑦ 마일리지<0
        );

        final Set<String> signatures = new LinkedHashSet<>();
        signatures.add(signatureOf(() -> calculator.calculate(null)));                                            // ① 요청 자체 null
        signatures.add(signatureOf(() -> calculator.calculate(new CalculateCartRequest(null, 0, 0))));            // ② 라인 목록 null
        signatures.add(signatureOf(() -> calculator.calculate(
                new CalculateCartRequest(Arrays.asList(validLine, null), 0, 0))));                                 // ③ 개별 라인 null
        for (final CalculateCartRequest request : violatingRequests) {
            signatures.add(signatureOf(() -> calculator.calculate(request)));
        }

        assertThat(signatures).hasSize(7);
    }

    private String signatureOf(final org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        final Throwable thrown = catchThrowable(action);
        return thrown.getClass().getName() + ":" + thrown.getMessage();
    }

    // U-5: 할인 로직이 전혀 개입하지 않는 최소 유효 경로(무할인 기준선) — §4-2 U-5의 대표값.
    // 상품 합계 10,000×1 = 10,000 + 배송비 3,000 = 13,000(§1 도메인 규칙만으로 독립 계산).
    @DisplayName("U-5: 라인 1개·쿠폰 0원·마일리지 0원 → 상품 합계 + 배송비 그대로(13,000원)")
    @Test
    void returns_product_total_plus_shipping_when_no_discount_applies() {
        final CalculateCartRequest request =
                new CalculateCartRequest(List.of(new CartLine("상품", 10_000L, 1)), 0, 0);

        final long finalAmount = calculator.calculate(request);

        assertThat(finalAmount).isEqualTo(13_000L);
    }

    // U-7: 임의(유효 범위 내) 입력에 대해 최종 결제 금액은 항상 0 이상이다(§1 불변식).
    // jqwik 등 property-based 라이브러리가 프로젝트에 없어 대표 샘플로 대체한다 —
    // 라인 0~여러 개, 극단적으로 큰 할인(쿠폰·마일리지가 상품 합계를 크게 초과)까지 포함해
    // 상한 초과·전액 소멸 등 음수로 새기 쉬운 경로를 대표한다.
    @DisplayName("U-7: 임의 입력(유효 범위 내)에 대해 최종 결제 금액은 항상 0 이상이다")
    @Test
    void final_amount_is_never_negative_for_representative_valid_inputs() {
        final List<CalculateCartRequest> samples = List.of(
                new CalculateCartRequest(List.of(), 0, 0),
                new CalculateCartRequest(List.of(new CartLine("상품", 1_000L, 1)), 0, 0),
                new CalculateCartRequest(
                        List.of(new CartLine("상품", 12_000L, 2), new CartLine("상품", 5_000L, 1)), 5_000, 26_000),
                new CalculateCartRequest(List.of(new CartLine("상품", 3_000L, 1)), 5_000, 0),
                new CalculateCartRequest(List.of(), 100_000, 100_000),
                new CalculateCartRequest(List.of(new CartLine("상품", 100_000L, 10)), 100_000, 100_000),
                new CalculateCartRequest(List.of(new CartLine("상품", 0L, 1)), 0, 0)
        );

        for (final CalculateCartRequest sample : samples) {
            assertThat(calculator.calculate(sample))
                    .describedAs("입력: %s", sample)
                    .isGreaterThanOrEqualTo(0L);
        }
    }
}
