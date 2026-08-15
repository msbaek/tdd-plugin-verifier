package com.example.cart;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 계산 규칙(§1)은 순수 함수로 남긴다 — 도메인 클래스에 Spring 애노테이션을 붙이지 않고
 * 여기서만 bean으로 등록한다.
 */
@Configuration
public class CartConfig {

    @Bean
    public CartCalculator cartCalculator() {
        return new CartCalculator();
    }
}
