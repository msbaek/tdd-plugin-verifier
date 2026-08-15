package com.example.cart;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * §2 Gherkin의 실행 원본({@code src/test/resources/com/example/cart/cart-checkout.feature})을
 * 구동하는 Runner. 미구현 시나리오는 {@code @pending} 태그로 제외한다 — §6 RGB가 진행되며
 * Green 단계마다 자기 시나리오의 태그를 해제한다.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("com/example/cart")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.cart")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @pending and not @api-enforced")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class RunCucumberTest {
}
