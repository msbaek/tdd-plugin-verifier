package com.example.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 라인 단독 조회. aggregate({@link CartRepository})를 거치지 않고 DB에 실제로 저장된 라인을
 * 확인하는 용도 — §5의 쓰기 누출 가드 테스트가 1차 캐시가 아니라 DB 상태를 보게 한다.
 */
public interface CartLineJpaRepository extends JpaRepository<CartLineEntity, Long> {

    List<CartLineEntity> findByCart_IdOrderByIdAsc(Long cartId);
}
