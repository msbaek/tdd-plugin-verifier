# msbaek-tdd 플러그인으로 Web App을 TDD로 구현하기 — 실전 튜토리얼

> 이 문서는 `tdd-plugin-verifier` 프로젝트에서 실제로 수행한 전 과정을 다른 사람이 그대로 따라 할 수 있도록 정리한 튜토리얼이다.
> 예제 도메인: **장바구니 결제 금액 계산** (상품 합계 → 쿠폰 할인 → 마일리지 차감 → 배송비 합산).
> 검증 환경: Claude Code + msbaek-tdd plugin 1.38.0, Spring Boot 3.3, Maven, Cucumber, Testcontainers(MySQL 8.4), Docker.

## 전체 흐름 한눈에 보기

```
[0] 요구사항 원천 문서 작성 (사람이 직접)
 │
[1] /tdd web-app <FQCN>          → Maven 프로젝트 + 8단계 템플릿(Cart.md) 생성
 │
[2] /tdd-plan @요구사항문서       → §1 도메인 규칙 → §2 Gherkin → §4 Unit Test 목록
 │    (에이전트 초안 → 사용자 승인 → 커밋, plan-critic 적대적 검증 게이트 통과까지 반복)
 │
[3] /cucumber-acceptance          → .feature + Runner + Four Layer, 전부 @pending, 빌드 그린
 │
[4] 인수 테스트 채널 결정          → in-process vs REST+JPA+MySQL 관통 (사용자 결정)
 │
[5] Walking Skeleton              → 실제 HTTP → Spring 앱 → MySQL(Testcontainers) 최소 관통
 │    (에이전트 자기 보고를 믿지 말고 mvn test 직접 재실행으로 검증)
 │
[6] /tdd-rgb                      → Red→Green→Blue 사이클로 계산 로직 구현
 │    (기어 low/mid/high에 따라 커밋 단위가 달라짐)
 │
[7] 적대적 리뷰 (adversarial-reviewer) → MAJOR 결함 발견·해소 후 완료
```

핵심 원칙 세 가지가 전 과정을 관통한다.

1. **정본(single source of truth) 규율** — 모든 숫자·규칙의 정본은 Cart.md §1(도메인 규칙 + 검산 전개)이고, Gherkin(§2)·unit test(§4)·코드는 전부 그 파생 뷰다. 숫자가 두 곳에 살면 반드시 한쪽을 정본으로 지정한다.
2. **승인 게이트** — 에이전트는 초안만 쓴다. 각 단계마다 사용자가 결정 사항(모호한 요구, 범위, 정책)에 답하고, 반영·커밋 후 다음 단계로 넘어간다.
3. **자체 보고 불신** — 서브에이전트의 "BUILD SUCCESS" 주장은 항상 직접 재실행으로 검증한다. 이 프로젝트에서 실제로 거짓 보고 1건을 잡았다.

---

## 0단계. 요구사항 원천 문서 작성

플러그인을 실행하기 전에, 사람이 도메인 규칙을 마크다운으로 먼저 적는다. 이 프로젝트의 `docs/calculate-cart-requirements.md` 전문:

```markdown
# 장바구니 합계 계산 기능

- 장바구니 금액 산출. 부수효과 없는 순수 계산.
- TDD 유형: web-app, rest, jpa, mysql

## 도메인 규칙 (0층)

- 기본 규칙
  - 장바구니는 0개 이상의 라인을 가진다. 라인 = (상품, 단가, 수량). 수량 ≥ 1.
  - **상품 합계** = 모든 라인의 (단가 × 수량) 합.
  - **배송비**는 주문당 고정 3,000원. 단, 빈 장바구니는 배송비도 0원.
  - 모든 금액은 원 단위 정수이며 음수가 될 수 없다.
- 특별 규칙 — 할인 적용 순서가 불변식
  - 적용 순서: **상품 합계 → 쿠폰 할인 → 마일리지 차감 → 배송비 합산**
  - **쿠폰 할인(1단계)**: 상품 합계에서 차감. 초과분은 소멸(상품 잔액은 0원 하한).
  - **마일리지 차감(2단계)**: 쿠폰 적용 후 잔액 기준.
    상품 잔액 → 0원 도달 시 배송비에서 추가 차감 → 그 이상은 미사용(차감 상한).
  - **최종 결제 금액** = 상품 잔액 + 배송비 잔액. 항상 ≥ 0.
```

작성 요령:

- **규칙을 "0층"으로 명시한다** — 이후 모든 예시·테스트가 이 층에서 파생된다는 선언이다.
- 완벽할 필요 없다. 모호한 부분(null 정책, 검사 순서, 출력 범위 등)은 다음 단계에서 에이전트가 질문으로 끌어내 준다.
- 대신 **핵심 불변식(여기서는 할인 적용 순서)은 반드시 명시**한다. 이것이 없으면 계획 전체가 흔들린다.

> 요구사항이 흩어진 메모 수준이라면 `/tdd-plan-input` 스킬로 질의응답을 통해 이 문서를 먼저 만들 수도 있다.

## 1단계. 프로젝트 생성 — `/tdd web-app <FQCN>`

```
/msbaek-tdd:tdd web-app com.example.cart.Cart
```

빈 디렉토리에서 실행하면 빌드 파일이 없다고 알려주며 선택지를 준다 → **"Maven 프로젝트 새로 생성"** 선택. 결과물:

| 산출물 | 내용 |
|---|---|
| `pom.xml` | Spring Boot 3.3.4 + Web/JPA/Validation + MySQL/H2 + Cucumber(java, junit-platform-engine, spring) + Testcontainers(mysql) |
| `CartApplication.java` | `@SpringBootApplication` 진입점 |
| `src/test/java/com/example/cart/Cart.md` | **web-app 8단계 템플릿** — 이후 모든 단계의 진행 기록·정본 문서 |
| `CartTest.java` | 빈 테스트 클래스 |

Cart.md 템플릿의 8단계 체크리스트:

```markdown
- [ ] 1. 요구사항 작성 (도메인 규칙 + User Story, 조건부 Use Case)
- [ ] 2. Gherkin Scenario 작성
- [ ] 3. 인수 테스트 셋업 (.feature + Runner, 미구현은 @pending)
- [ ] 4. Unit Test 목록 작성
- [ ] 5. Walking Skeleton 구현
- [ ] 6. 테스트 구현 (RGB 사이클 — 각 Green이 자기 시나리오 @pending 해제)
- [ ] 7. JPA Repository 완성
- [ ] 8. DSL 개선 (Steps·Protocol Driver·Test Data Builder)
```

이 시점에 첫 커밋을 남긴다. 이후 매 단계 승인마다 커밋해서 언제든 되돌릴 수 있게 한다(rollback-friendly).

## 2단계. 계획 수립 — `/tdd-plan @요구사항문서`

```
/msbaek-tdd:tdd-plan @docs/calculate-cart-requirements.md
```

이 스킬은 세 개의 전문 에이전트를 순차 호출하고, **각 단계마다 사용자 승인을 받은 뒤 커밋**한다.

### 2-1. 도메인 규칙 + User Story (`tdd-domain-modeler`)

에이전트가 Cart.md §1에 초안을 쓴다. 핵심 산출물 두 가지:

- **도메인 규칙 0층** — 요구사항을 정본 규칙으로 정련.
- **검산 전개(숫자의 정본)** — 대표 입력 1건을 손으로 계산한 표.
  이 프로젝트에서는: 상품 29,000 → 쿠폰 5,000 차감 → 24,000 → 마일리지 26,000이 상품(24,000)+배송비(2,000)에 걸쳐 차감 → **최종 1,000원**.
  이후 모든 Gherkin `Examples` 수치는 이 표에서만 나온다.

에이전트는 확신할 수 없는 사항을 **지어내지 않고 질문으로 올린다**. 이 프로젝트에서 실제로 나온 결정 5건:

| 질문 | 사용자 결정 |
|---|---|
| US-2(금액 구성 내역 노출)를 스토리로 유지? | 삭제 — US-1(최종 금액)만 |
| 미사용 마일리지를 응답에 포함? | 포함 안 함 |
| 입력 검증 책임은 누가? | 계산기가 검증(단, 잔고·쿠폰 유효기간 등 외부 상태 의존 검증은 범위 밖) |
| 순수 POJO만? REST+JPA+MySQL까지? | REST+JPA+MySQL까지 포함 |

결정을 문서에 반영하고 커밋: `docs: 요구사항(도메인 규칙 + User Story) 작성 - Cart`

### 2-2. Gherkin Scenario (`tdd-example-designer`)

§1의 규칙·검산 표를 Gherkin `Rule` + `Scenario`로 변환한다(이 프로젝트: Rule 6개, 시나리오 12개 초안 → 최종 15개). 경계 조건 5종(수치·크기·상태·시간·집계)을 스캔해 빠진 경계를 찾는다.

여기서도 정본 규율이 작동한다. 예: E-6 시나리오의 기대값 3,000원이 §1 검산 표에 없자, 에이전트가 "정본 이중화" 문제로 보고 → 사용자 승인 후 **§1에 부속 검산 표를 보간**하고 나서야 E-6을 확정했다.

커밋: `docs: Gherkin Scenario 작성 - Cart`

### 2-3. Unit Test 목록 (`tdd-test-list`)

Gherkin이 이미 검증하는 external behavior는 **unit test로 다시 쓰지 않는다**(두 계층 중복 금지). Gherkin으로 표현 못 하는 세밀 분기만 U-* 항목으로 추가:

- null 방어(요청 자체 null 등), 여러 위반 동시 성립 시 우선순위, 무할인 기준선, property-based 불변식(최종 금액 ≥ 0).

그리고 전체(E-* + U-*)를 **Degenerate → General 순서**(가장 퇴화한 케이스부터 일반화)로 정렬한 "통합 RGB 순서" 체크리스트를 만든다. 이것이 6단계 구현 순서가 된다.

이 단계에서도 결정 3건이 올라왔다(동시 위반 처리 → 첫 위반만 보고, 오버플로 방어 → 범위 밖, 요청 null → Walking Skeleton 이후 재판단으로 보류).

### 2-4. 적대적 검증 게이트 (`tdd-plan-critic`)

§1~§4가 모두 승인되면 **읽기 전용 적대적 검증자**가 문서를 깨뜨리러 든다 — 정본 부재, 모순, 중복, 경계 누락을 찾는다. 이 프로젝트의 실제 진행:

1. **1차: 게이트 통과 불가** (critical 3건) — 범위 모순(§1 "Small" 근거 vs REST 포함), null 정책 미명시, 검사 순서 미명시 → 사용자 결정 3건 받아 §1·§2·§4를 각 담당 에이전트로 보완.
2. **2차: 통과 불가** (critical 1건) — "라인 객체 null"과 "라인 필드 null"을 한 항목에 뭉뚱그린 모순 → 라인 객체 null만으로 좁혀 직접 수정.
3. **3차: PASS** — non-blocking major 3건은 즉시 수정 후 커밋.

**게이트를 통과할 때까지 코드를 쓰지 않는다.** 이 반복이 비싸 보이지만, 뒤 단계(6·7)에서 세 번의 독립 리뷰가 같은 문장을 서로 반대로 읽는 사건이 실제로 벌어졌다 — 계획 단계 모호성은 반드시 구현 단계 비용으로 돌아온다.

## 3단계. 인수 테스트 셋업 — `/cucumber-acceptance`

```
/msbaek-tdd:cucumber-acceptance 실행해줘
```

`tdd-acceptance-builder` 에이전트가 §2 Gherkin을 실행 가능한 형태로 옮긴다:

- `src/test/resources/com/example/cart/cart-checkout.feature` — §2를 **그대로 옮긴 실행 원본**(재작성 금지). 15개 실행 단위 전부 `@pending`.
- **Four Layer 구조**: `RunCucumberTest`(Runner) → `CartCalculationSteps`(Steps) → `CartCalculationDriver`(Protocol Driver) → SUT.
- Runner는 `@ConfigurationParameter(FILTER_TAGS_PROPERTY_NAME, "not @pending")`으로 미구현 시나리오를 가역적으로 제외.
- `mvn test` → BUILD SUCCESS, 15 skipped(의도됨), undefined step 없음 확인 후 커밋.

`.feature`가 실행되므로 **문서와 코드의 드리프트가 구조적으로 차단**된다 — §2가 바뀌면 `.feature`도 같은 커밋에서 바뀐다.

## 4단계. 인수 테스트 채널 결정 (중요한 갈림길)

셋업 직후 사용자가 물었다: "인수 테스트도 REST, JPA, MySQL을 사용하는지 확인해줘."

확인 결과 **아니었다** — 기본 셋업의 Driver는 `new CartCalculator()`를 직접 호출하는 in-process 방식이다(스킬의 기본 권장: "주 검증층은 빨라야 한다"). 이것은 버그가 아니라 설계 기본값이므로, 원하는 방식이 다르면 **명시적으로 결정을 뒤집고 문서화**해야 한다.

사용자 결정 2건:

1. 인수 테스트는 **실제 HTTP → 실제 Spring 앱 → 실제 MySQL(Testcontainers)을 관통**한다.
2. REST API는 요청 본문에 라인을 담지 않고 **장바구니 ID로 DB에서 라인을 조회**한다(POST는 `cartId·coupon·mileage`만; Given 스텝이 라인을 DB에 시드).

이 결정을 Cart.md §1에 "인수 테스트 채널 결정" 절로 기록하고 커밋했다. 파급 효과도 함께 적었다 — 계산기는 순수 함수로 남고, Controller가 cartId → Repository 조회 → `CalculateCartRequest` 조립을 맡는다.

> 교훈: 스킬 기본값과 내 의도가 다를 수 있다. 산출물이 나오면 "내가 원하는 방식인가"를 반드시 확인하고, 다르면 결정을 정본 문서에 남긴다.

## 5단계. Walking Skeleton — 가장 얇은 E2E 관통

`tdd-skeleton-builder` 에이전트에 위임한다. 산출물:

- JPA 엔티티(`Cart` aggregate root + `CartLineEntity`, LAZY), `CartRepository`(`@EntityGraph` 조회)
- `CartCheckoutController` — `POST /carts/{cartId}/checkout`
- Testcontainers MySQL 연결, 인수 테스트 Driver를 `TestRestTemplate` + DB 시드로 교체
- `CartCheckoutWriteLeakGuardTest` — 읽기 전용 트랜잭션 경계(save 누출) 가드 테스트

계산 로직은 아직 `UnsupportedOperationException`을 던지는 스텁이다. **관통이 목적이지 완성이 목적이 아니다.**

### 자체 보고 검증 — 이 튜토리얼에서 가장 중요한 습관

에이전트는 "BUILD SUCCESS"를 보고했지만, IDE 진단에 컴파일 에러가 떠서 직접 재검증했다:

1. `mvn compile` → 성공 (IDE 진단은 편집 중간 스냅샷의 잔상이었음)
2. `mvn test` → **BUILD FAILURE** — 가드 테스트가 404를 받음. **에이전트 보고가 거짓이었다.**
3. 반복 실행으로 특성 파악: 총 13회 중 12회 통과, 실패는 에이전트 작업 직후 첫 1회뿐 → 컨테이너 콜드스타트 플레이키로 판정하고 진행.

> 규칙: **서브에이전트의 성공 주장은 증거가 아니라 가설이다.** 항상 자기 손으로 `mvn test`를 다시 돌린다. 한 번 성공해도 플레이키가 의심되면 여러 번 돌려 통과율로 판단한다.

## 6단계. RGB 사이클 — `/tdd-rgb` (3개 기어 비교)

이 프로젝트는 플러그인 검증 목적으로 **같은 use case를 3개 브랜치 × 3개 기어로 각각 구현**했다. 일반 사용이라면 기어 하나만 고르면 된다. 기어는 커밋 단위(=사용자 개입 밀도)를 결정한다:

| 기어 | 커밋 단위 | 이 프로젝트 결과 |
|---|---|---|
| **low** | R/G/B 각 phase마다 커밋 | 39 커밋 |
| **mid** | 테스트 사이클(R+G+B)마다 커밋 | 7 커밋 |
| **high** | use case 하나 = 커밋 하나 (R/G/B 에이전트는 `git add`까지만, 오케스트레이터가 마지막에 한 번 커밋) | 2 커밋 (use case 1 + 리뷰 수정 1) |

시작 전 공통 준비:

```bash
git branch tdd-rgb-low tdd-rgb-mid tdd-rgb-high   # 깨끗한 main에서 분기
```

Cart.md §6 진행 기록에 기어·시작 커밋을 명시한다: `기어: low (폭발 반경 high-stakes: 금액 계산 — 완료 시 적대적 리뷰) / 시작 커밋: a4ae5bd`. 금액 계산은 high-stakes 도메인이므로 **기어와 무관하게 완료 시 적대적 리뷰 1회가 필수**다.

### low 기어의 실제 진행 (표준 RGB의 교과서적 기록)

§4의 통합 RGB 순서(Degenerate → General)를 따라 한 항목씩:

```
E-7  빈 장바구니 → 0원          : Red(실패 확인·커밋) → Green(return 0 — Fake It) → 커밋
U-1  요청 null 거부             : Red → Green(null 체크 추가) → 커밋
E-14/15 라인 null 2종           : REST 채널로는 표현 불가 판정 → @api-enforced 재태깅
                                  + 순수 함수 계약은 CartCalculatorTest 단위 테스트로 이관
E-9~E-12 필드 검증 4종          : 시나리오마다 @pending 해제(Red) → 검증 1개 추가(Green) → 커밋
                                  4개 끝난 뒤 Blue: validate() 메소드 추출 (Composed Method)
E-16, E-13, U-9, U-4, U-8       : 대부분 "이미 green" — 기존 구현이 커버함을 확인하고
                                  테스트만 추가하는 test-only 커밋 (체크리스트에 사유 명기)
U-5  무할인 기준선 13,000원      : Red → Green(상품 합계 + 배송비 — 진짜 계산 시작)
E-6  쿠폰 초과분 소멸            : Red → Green(Math.max(0, total - coupon))
E-2  마일리지 상품 잔액 내 차감  : Red → Green(2단계 clamp)
E-4  상한 정확 소진              : Red → Green(마일리지 잔여분의 배송비 이월)
                                  Blue: sumProductTotal() 추출, 낡은 javadoc 정리
E-5, E-1, E-8                   : 이미 green (clamp·순회가 커버) → test-only 커밋
U-7  property 불변식(≥0)         : 고정 시드 랜덤 500회 샘플링 — 즉시 통과
```

따라 할 때의 요점 4가지:

1. **Red를 반드시 눈으로 확인하고 커밋한다.** 실패하지 않는 테스트는 아무것도 증명하지 않는다.
2. **`@pending` 해제가 Red다** — Gherkin 항목은 `.feature`의 태그를 지우는 것이 실패 테스트 추가와 같다. Green과 같은 커밋 안에서 자기 시나리오만 해제한다.
3. **"이미 green"은 정상이다.** 앞선 일반화가 뒤 케이스를 덮으면 신규 프로덕션 코드 없이 test-only 커밋으로 행동을 고정하고, 체크리스트에 "E-9 구현의 전체 라인 순회가 이미 커버" 같은 사유를 남긴다.
4. **채널로 표현 불가능한 시나리오는 정직하게 처리한다.** E-14/E-15(라인 null)는 cartId 기반 REST 채널로 구성 자체가 불가능 → `@api-enforced` 태그로 제외하되, Runner 필터에 태그를 추가하고 순수 함수 계약은 단위 테스트로 검증한다. "만족됨"이 아니라 "구성 불가(inexpressible)"라고 정확히 기록한다.

### mid·high 기어에서의 주의점 — 검증 극장 회피

두 번째(mid)·세 번째(high) 브랜치는 앞 브랜치의 리뷰가 잡은 결함 수정을 **처음부터 반영하고 시작**했다. 아는 버그를 일부러 재도입한 뒤 다시 "발견"하는 것은 검증 극장(verification theater)이기 때문이다. 대신 각 브랜치가 **자체적으로 새 적대적 리뷰**를 받았고, 그 사유를 Cart.md에 기록했다.

high 기어 검증 포인트: use case당 커밋이 정확히 하나인지, R/G/B 에이전트가 커밋을 보류하고 `git add`까지만 했는지, 마지막 `git status`가 깨끗한지.

### §7·§8 마무리

- **§7 JPA Repository**: 이 프로젝트는 InMemory 프로파일을 도입한 적이 없어 "계약 테스트로 동등성 검증"할 두 번째 구현이 없다 → InMemory를 새로 만드는 것은 범위 지어내기(No overengineering)로 판단, **범위 결정 자체를 문서화**하고 §5 산출물(엔티티·Repository·가드 테스트)로 완료 처리.
- **§8 DSL 개선**: Test Data Builder 도입 — `aLine(unitPrice, quantity)` 헬퍼로 계산이 쓰지 않는 상품명을 감추고 반복 제거.

## 7단계. 적대적 리뷰 — 세 번의 독립 리뷰가 각각 다른 결함을 찾았다

각 브랜치 완료 시 `adversarial-reviewer` 에이전트에 시작 커밋..HEAD diff를 주고 "이 구현이 실패하는 구체적 시나리오"를 찾게 했다. 결과가 이 튜토리얼의 백미다:

| 리뷰 | 발견한 MAJOR | 해소 방법 |
|---|---|---|
| low | ① 예외 핸들러 부재 — 입력 위반이 **500으로 새는데** 인수 테스트가 "not 2xx"만 봐서 정상 거부로 오인 | 단언을 4xx 검사로 강화해 버그를 Red로 재현 → `@ExceptionHandler`로 400 매핑 추가 |
| low | ② `@OrderBy` 부재 — REST 채널에서 라인 순서가 DB 수준에서 미보장 | `@OrderBy("id ASC")` 추가 |
| mid | ① low의 수정이 또 다른 구멍 — `is4xxClientError()`가 **404(장바구니 없음)까지 정상 거부로 삼킴** | `BAD_REQUEST` 정확 일치로 좁히고 404≠400 증명 테스트 추가 |
| mid | ② `@OrderBy` 주석의 거짓 주장 — "E-16이 이 순서를 검증한다"고 적었으나 계산 로직은 라인 순서에 의존하지 않음 | 주석을 "방어적 결정성일 뿐 도메인 계약 아님"으로 정직하게 정정 |
| high | ① §1 검사 순서 문장을 **세 번의 독립 리뷰가 매번 반대로 읽음**(field-major vs line-major) | 사용자 재확인으로 field-major 확정 → §1을 구체 예시 3개로 재작성해 네 번째 오독을 구조적으로 차단 |
| high | ② §1 대표 예시(`lines=[(수량0), null]`)가 테스트로 잠겨 있지 않음 — null 우선 로직을 지워도 아무 테스트도 안 깨짐 | 예시 그대로의 고정 테스트 추가 |

여기서 배울 것:

- **리뷰 지적도 검증 대상이다.** low 리뷰의 MAJOR ②(필드 우선순위 충돌 우려)는 재확인 결과 구현이 이미 옳았다 — 그래도 리뷰어의 반례를 고정 테스트로 추가해 우려를 영구히 해소했다.
- **같은 코드에 리뷰를 여러 번 돌리면 매번 다른 것이 나온다.** 특히 "앞 리뷰의 수정이 만든 새 구멍"(4xx가 404를 삼킴)은 한 번의 리뷰로는 절대 못 잡는다.
- **반복해서 오독되는 문장은 산문을 고치지 말고 예시(worked example)를 박아라.** 규칙 문장은 세 번 오독됐지만, 입력→기대 예외를 명시한 예시 3개는 오독의 여지가 없다.

## 최종 결과

| 기어 | 브랜치 | 커밋 수 | 커밋 단위 | 적대적 리뷰 |
|---|---|---|---|---|
| low | `tdd-rgb-low` | 39 | R/G/B phase마다 | MAJOR 2건 발견·해소 |
| mid | `tdd-rgb-mid` | 7 | 테스트 사이클마다 | MAJOR 2건 (low와 다른 결함) |
| high | `tdd-rgb-high` | 2 | use case 1개 (+리뷰 수정 1개) | MAJOR 2건 (3번째 독립 발견) |

세 브랜치 모두 `mvn test` 전부 green(인수 15 시나리오 + 단위 테스트, `@api-enforced` 2건은 의도된 제외), `git status` 전부 clean.

## 따라 하기 체크리스트 (요약)

1. 도메인 규칙 0층이 담긴 요구사항 문서를 먼저 쓴다. Docker를 켜 둔다.
2. `/tdd web-app <FQCN>` → 프로젝트 + 8단계 템플릿 생성, 커밋.
3. `/tdd-plan @요구사항문서` → 에이전트 초안마다 결정 질문에 답하고 커밋, plan-critic 게이트를 **PASS까지** 반복.
4. `/cucumber-acceptance` → 전부 `@pending`으로 빌드 그린 확인. **Driver가 원하는 채널(in-process vs REST+DB)인지 직접 확인**하고, 다르면 결정을 문서화한다.
5. Walking Skeleton 관통 후 에이전트 보고와 무관하게 `mvn test`를 직접(필요하면 여러 번) 돌린다.
6. `/tdd-rgb` — 기어를 고르고, Degenerate→General 순서로 한 항목씩. Red는 눈으로 확인하고, "이미 green"은 사유와 함께 test-only 커밋.
7. 완료 시 적대적 리뷰를 돌리고(고위험 도메인은 필수), MAJOR는 Red 재현 → 수정 → Green으로 처리한다. 반복 오독되는 규칙 문장은 예시로 재작성한다.
