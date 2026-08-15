package com.example.cart;

import io.cucumber.spring.ScenarioScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;

/**
 * Protocol Driver를 bean으로 등록한다. {@code @ScenarioScope}이므로 시나리오마다 새로
 * 만들어져 상태가 다음 시나리오로 새지 않는다.
 */
@TestConfiguration
public class CartAcceptanceTestConfig {

    @Bean
    @ScenarioScope
    public CartCalculationDriver cartCalculationDriver(final TestRestTemplate restTemplate,
                                                        final CartRepository cartRepository) {
        return new CartCalculationDriver(restTemplate, cartRepository);
    }
}
