package com.example.cart;

import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps — 파싱과 {@link CartCalculationDriver} 위임만 한다. SUT와의 상호작용 코드(HTTP 호출·
 * DB 시드)는 이 클래스에 두지 않는다(Protocol Driver 분리). 채널이 in-process 계산 호출에서
 * REST+JPA+MySQL로 바뀌었지만 이 클래스는 driver 주입 방식만 바뀌었다.
 */
public class CartCalculationSteps {

    @Autowired
    private CartCalculationDriver driver;

    @Given("장바구니에 단가 {int}원 상품이 {int}개 담겨 있다")
    public void 장바구니에_상품이_담겨_있다(final long unitPrice, final int quantity) {
        driver.addLine(unitPrice, quantity);
    }

    @Given("장바구니에 같은 단가 {int}원 상품이 {int}개 더 담겨 있다")
    public void 장바구니에_같은_상품이_더_담겨_있다(final long unitPrice, final int quantity) {
        driver.addLine(unitPrice, quantity);
    }

    @Given("장바구니가 비어 있다")
    public void 장바구니가_비어_있다() {
        // no-op: driver의 라인 목록은 기본값이 빈 리스트다.
    }

    @Given("쿠폰 {int}원을 사용한다")
    public void 쿠폰을_사용한다(final long coupon) {
        driver.setCoupon(coupon);
    }

    @Given("마일리지 {int}원을 사용한다")
    public void 마일리지를_사용한다(final long mileage) {
        driver.setMileage(mileage);
    }

    @Given("장바구니 라인 목록이 null인 계산 요청이 준비되어 있다")
    public void 라인_목록이_null인_요청이_준비되어_있다() {
        driver.makeLinesNull();
    }

    @Given("장바구니에 null인 라인이 하나 더 담겨 있다")
    public void null인_라인이_담겨_있다() {
        driver.addNullLine();
    }

    @Given("장바구니에 단가 {int}원 상품이 {int}개 담긴 계산 요청이 준비되어 있다")
    public void 단일_라인_계산_요청이_준비되어_있다(final long unitPrice, final int quantity) {
        driver.addLine(unitPrice, quantity);
    }

    @Given("쿠폰 {int}원, 마일리지 {int}원이 주어져 있다")
    public void 쿠폰과_마일리지가_주어져_있다(final long coupon, final long mileage) {
        driver.setCoupon(coupon);
        driver.setMileage(mileage);
    }

    @Given("그 요청의 {word}가 {int}으로 덮어써진다")
    public void 요청의_필드가_덮어써진다(final String field, final long value) {
        driver.overwriteField(field, value);
    }

    @Given("그 두 번째 라인의 수량이 {int}으로 덮어써진다")
    public void 두번째_라인의_수량이_덮어써진다(final int quantity) {
        driver.overwriteLineQuantity(2, quantity);
    }

    @When("결제 금액 계산을 요청하면")
    public void 결제_금액_계산을_요청한다() {
        driver.calculate();
    }

    @Then("최종 결제 금액은 {int}원이다")
    public void 최종_결제_금액을_확인한다(final long expectedAmount) {
        assertThat(driver.finalAmount()).isEqualTo(expectedAmount);
    }

    @Then("계산이 거부된다")
    public void 계산이_거부됨을_확인한다() {
        assertThat(driver.wasRejected()).isTrue();
    }
}
