package com.example.cart;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Cucumber-Spring 통합 지점. 인수 테스트는 실제 임베디드 서버(RANDOM_PORT) + 실제
 * MySQL(Testcontainers)을 관통한다 — in-process 계산 호출이 아니다(Cart.md §1 채널 결정).
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Import(CartAcceptanceTestConfig.class)
public class CucumberSpringConfiguration extends MySqlTestContainer {
}
