package com.example.cart;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * 실제 MySQL 컨테이너(싱글턴). JUnit의 {@code @Container} 대신 static 초기화 + 수동
 * {@code start()}를 쓴다 — 여러 테스트 클래스(Cucumber Runner, 가드 테스트)가 하나의
 * 컨테이너를 재사용하고 JVM 종료 시 Ryuk이 정리한다.
 *
 * <p>임베디드 DB로 자동 대체되지 않는지는 실행 로그의 접속 URL(jdbc:mysql://...)로 확인한다.
 */
public abstract class MySqlTestContainer {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("cart")
            .withUsername("cart")
            .withPassword("cart");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }
}
