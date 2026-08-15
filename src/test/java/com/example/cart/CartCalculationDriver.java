package com.example.cart;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Protocol Driver — Cucumber Steps와 SUT 사이의 유일한 상호작용 지점.
 *
 * <p>채널: 실제 HTTP({@link TestRestTemplate}) → 실제 Spring 앱 → 실제
 * MySQL(Testcontainers). Given("… 담겨 있다")은 MySQL에 라인을 시드하는 동작이고,
 * When("계산을 요청하면")은 {@code POST /carts/{cartId}/checkout} 호출이다.
 * Steps는 이 클래스의 공개 메서드 시그니처만 알면 되므로 채널 교체에도 바뀌지 않았다.
 */
class CartCalculationDriver {

    private static final String DEFAULT_PRODUCT_NAME = "상품";

    private final TestRestTemplate restTemplate;
    private final CartRepository cartRepository;

    /** 시드 대기 중인 라인 사양. 실제 저장은 calculate() 시점에 한 번에 일어난다. */
    private final List<LineSpec> lines = new ArrayList<>();
    private boolean linesNull = false;
    private boolean hasNullLine = false;
    private long coupon = 0;
    private long mileage = 0;

    private Long finalAmount;
    private boolean rejected;
    private org.springframework.http.HttpStatusCode lastStatusCode;
    private String lastStatus = "(요청 전)";

    CartCalculationDriver(final TestRestTemplate restTemplate, final CartRepository cartRepository) {
        this.restTemplate = restTemplate;
        this.cartRepository = cartRepository;
    }

    void addLine(final long unitPrice, final int quantity) {
        lines.add(new LineSpec(DEFAULT_PRODUCT_NAME, unitPrice, quantity));
    }

    void addNullLine() {
        hasNullLine = true;
    }

    void makeLinesNull() {
        this.linesNull = true;
    }

    void setCoupon(final long coupon) {
        this.coupon = coupon;
    }

    void setMileage(final long mileage) {
        this.mileage = mileage;
    }

    /** E-9~E-12: 요청의 한 필드(수량/단가/쿠폰/마일리지)를 지정된 값으로 덮어쓴다. */
    void overwriteField(final String field, final long value) {
        switch (field) {
            case "수량" -> overwriteLineQuantity(lines.size(), (int) value);
            case "단가" -> overwriteLastLineUnitPrice(value);
            case "쿠폰" -> this.coupon = value;
            case "마일리지" -> this.mileage = value;
            default -> throw new IllegalArgumentException("알 수 없는 필드: " + field);
        }
    }

    /** E-16: n번째(1-based) 라인의 수량만 덮어쓴다. */
    void overwriteLineQuantity(final int oneBasedIndex, final int quantity) {
        final int index = oneBasedIndex - 1;
        final LineSpec old = lines.get(index);
        lines.set(index, new LineSpec(old.product(), old.unitPrice(), quantity));
    }

    void calculate() {
        final Long cartId = seedCart();
        final ResponseEntity<CheckoutResponse> response = restTemplate.postForEntity(
                "/carts/{cartId}/checkout",
                new CheckoutRequest(coupon, mileage),
                CheckoutResponse.class,
                cartId);

        lastStatusCode = response.getStatusCode();
        lastStatus = response.getStatusCode().toString();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            finalAmount = response.getBody().finalAmount();
            rejected = false;
        } else {
            finalAmount = null;
            rejected = true;
        }
    }

    long finalAmount() {
        if (finalAmount == null) {
            throw new IllegalStateException(
                    "계산이 성공하지 않아 최종 금액이 없다 — 마지막 HTTP 응답: " + lastStatus);
        }
        return finalAmount;
    }

    boolean wasRejected() {
        return rejected;
    }

    /**
     * 입력 유효성 위반이 정확히 400(Bad Request)으로 거부됐는지 확인한다.
     * 5xx(서버 오류)를 거부로 오인하지 않는 것은 물론, 404(장바구니 없음)도 별개
     * 실패 사유이므로 뭉뚱그리지 않는다 — mid 적대적 리뷰 MAJOR #1.
     */
    boolean wasRejectedAsClientError() {
        return rejected && org.springframework.http.HttpStatus.BAD_REQUEST.equals(lastStatusCode);
    }

    /**
     * Given 스텝이 쌓아 둔 라인을 MySQL에 시드하고 장바구니 ID를 돌려준다.
     * 수량 0·단가 음수 같은 위반 값도 그대로 저장한다 — §1에서 그 검증은 계산기의 책임이므로
     * DB 제약이 먼저 막으면 E-9·E-10을 검증할 수 없다.
     */
    private Long seedCart() {
        if (linesNull || hasNullLine) {
            throw new UnsupportedOperationException(
                    "라인 목록 null(E-14)·null 라인(E-15)은 REST+DB 채널로 표현할 수 없다 — "
                            + "라인은 요청 본문이 아니라 DB에서 오므로 null이 될 수 없다. "
                            + "이 두 케이스의 처리 방식은 Cart.md §5 미결 항목으로 §6 RGB에서 확정한다.");
        }
        final Cart cart = Cart.empty();
        lines.forEach(line -> cart.addLine(line.product(), line.unitPrice(), line.quantity()));
        return cartRepository.save(cart).getId();
    }

    private void overwriteLastLineUnitPrice(final long unitPrice) {
        final int lastIndex = lines.size() - 1;
        final LineSpec old = lines.get(lastIndex);
        lines.set(lastIndex, new LineSpec(old.product(), unitPrice, old.quantity()));
    }

    private record LineSpec(String product, long unitPrice, int quantity) {
    }
}
