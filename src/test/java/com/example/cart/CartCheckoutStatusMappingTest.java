package com.example.cart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * 적대적 리뷰(mid) MAJOR #1 — 4xx 하나로 뭉뚱그리면 "장바구니 없음(404)"과 "입력 유효성
 * 위반(400)"이 구분되지 않는다. 존재하지 않는 장바구니는 400이 아니라 정확히 404여야 한다.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("존재하지 않는 장바구니는 404이지 400이 아니다")
class CartCheckoutStatusMappingTest extends MySqlTestContainer {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void 존재하지_않는_장바구니는_404를_반환한다() {
        final ResponseEntity<CheckoutResponse> response = restTemplate.postForEntity(
                "/carts/{cartId}/checkout", new CheckoutRequest(0L, 0L), CheckoutResponse.class, 999_999_999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
