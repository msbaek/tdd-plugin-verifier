package com.example.cart;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * 쓰기 누출 가드(회귀 테스트) — 조회 경로인 {@code POST /carts/{cartId}/checkout}가
 * 트랜잭션 안에서 로딩한 엔티티를 건드려도 그 변경이 dirty checking으로 DB에 새지 않는지 본다.
 * 저장은 언제나 명시적 {@code save()}로만 일어나야 한다(§5 영속성 경계 결정).
 *
 * <p>이 테스트를 Controller에 트랜잭션 경계를 얹는 같은 변경에 동봉한다 — 경계 없이 가드만
 * 만들면 위험 경로를 한 번도 실행하지 않는 공허한 검증이 된다.
 *
 * <p>비공허성 확인(실패 주입, 수동): Controller의 조회 직후에
 * {@code cart.getLines().get(0).changeQuantity(99)}를 넣고
 * {@code @Transactional(readOnly = true)}에서 {@code readOnly}를 떼면 flush가 일어나 이
 * 테스트가 빨간불이 된다. {@code readOnly = true}를 되돌리면 다시 초록불이다.
 *
 * <p>계산 로직은 아직 없으므로({@link CartCalculator}는 UnsupportedOperationException)
 * 계산기만 stub으로 대체한다 — 이 테스트가 보는 것은 계산 결과가 아니라 영속성 경계다.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("체크아웃 조회 경로는 DB에 쓰지 않는다")
class CartCheckoutWriteLeakGuardTest extends MySqlTestContainer {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartLineJpaRepository cartLineJpaRepository;

    @MockBean
    private CartCalculator calculator;

    @AfterEach
    void tearDown() {
        cartRepository.deleteAll();
    }

    @Test
    void 체크아웃_요청_후에도_장바구니_라인은_그대로다() {
        given(calculator.calculate(any())).willReturn(0L);

        final Cart cart = Cart.empty();
        cart.addLine("상품", 12_000L, 2);
        final Long cartId = cartRepository.save(cart).getId();

        final ResponseEntity<CheckoutResponse> response = restTemplate.postForEntity(
                "/carts/{cartId}/checkout", new CheckoutRequest(0L, 0L), CheckoutResponse.class, cartId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 1차 캐시가 아니라 DB 상태를 본다 — aggregate가 아닌 라인 repository로 직접 조회한다.
        final List<CartLineEntity> persisted = cartLineJpaRepository.findByCart_IdOrderByIdAsc(cartId);
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getQuantity()).isEqualTo(2);
        assertThat(persisted.get(0).getUnitPrice()).isEqualTo(12_000L);
    }
}
