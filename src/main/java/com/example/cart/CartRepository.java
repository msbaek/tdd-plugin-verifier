package com.example.cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 장바구니 조회/저장. 라인은 LAZY이므로 조회 지점에서 {@code @EntityGraph}로 명시적으로 당긴다 —
 * OSIV가 꺼져 있어(§5) 트랜잭션 밖에서는 지연 로딩이 불가능하다.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = "lines")
    Optional<Cart> findWithLinesById(Long id);
}
