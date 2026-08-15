package com.example.cart;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Walking Skeleton의 얇은 어댑터. 하는 일은 세 가지뿐이다 —
 * ① {@code cartId}로 라인을 조회하고, ② {@link CalculateCartRequest}로 옮겨 담고,
 * ③ {@link CartCalculator}에 위임한다. 계산 규칙(§1)은 여기에 두지 않는다.
 *
 * <p>영속성 경계(§5 결정):
 * <ul>
 *   <li>트랜잭션 경계는 이 Controller 메서드다(이 단계 한정). 조회 전용이므로
 *       {@code readOnly = true} — Hibernate flush가 MANUAL이 되어 의도치 않은 dirty checking
 *       쓰기가 DB로 새지 않는다(가드 테스트: {@code CartCheckoutWriteLeakGuardTest}).</li>
 *   <li>라인은 LAZY이며 {@code findWithLinesById}의 {@code @EntityGraph}로만 당긴다.</li>
 *   <li>응답은 엔티티가 아니라 {@link CheckoutResponse} DTO다.</li>
 * </ul>
 */
@RestController
public class CartCheckoutController {

    private final CartRepository cartRepository;
    private final CartCalculator calculator;

    public CartCheckoutController(final CartRepository cartRepository, final CartCalculator calculator) {
        this.cartRepository = cartRepository;
        this.calculator = calculator;
    }

    @PostMapping("/carts/{cartId}/checkout")
    @Transactional(readOnly = true)
    public ResponseEntity<CheckoutResponse> checkout(@PathVariable final Long cartId,
                                                     @RequestBody final CheckoutRequest request) {
        final Cart cart = cartRepository.findWithLinesById(cartId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "장바구니를 찾을 수 없다: " + cartId));

        final List<CartLine> lines = cart.getLines().stream()
                .map(line -> new CartLine(line.getProduct(), line.getUnitPrice(), line.getQuantity()))
                .toList();

        final long finalAmount = calculator.calculate(new CalculateCartRequest(lines, request.coupon(), request.mileage()));

        return ResponseEntity.ok(new CheckoutResponse(finalAmount));
    }
}
