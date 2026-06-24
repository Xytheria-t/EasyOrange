package com.cartethyia.easyorange.product.domain.service;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.valueobject.OfferDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OfferRuleEngine 测试")
class OfferRuleEngineTest {

    private static final Money FLOOR_PRICE = Money.of(new BigDecimal("100"));
    private static final Money CURRENT_PRICE = Money.of(new BigDecimal("100"));

    private OfferRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new OfferRuleEngine();
    }

    @Test
    @DisplayName("出价等于底价 → ACCEPT")
    void offerEqualToFloorPrice_shouldAccept() {
        OfferDecision decision = engine.evaluate(FLOOR_PRICE, FLOOR_PRICE, CURRENT_PRICE, 0);

        assertThat(decision.type()).isEqualTo(OfferDecision.DecisionType.ACCEPT);
        assertThat(decision.acceptedPrice()).isEqualByComparingTo("100");
        assertThat(decision.counterPrice()).isNull();
    }

    @Test
    @DisplayName("出价高于底价 → ACCEPT")
    void offerAboveFloorPrice_shouldAccept() {
        Money offer = Money.of(new BigDecimal("120"));
        OfferDecision decision = engine.evaluate(offer, FLOOR_PRICE, CURRENT_PRICE, 0);

        assertThat(decision.type()).isEqualTo(OfferDecision.DecisionType.ACCEPT);
        assertThat(decision.acceptedPrice()).isEqualByComparingTo("120");
        assertThat(decision.counterPrice()).isNull();
    }

    @Test
    @DisplayName("出价等于底价×0.95 → COUNTER（counterPrice = 底价×0.95）")
    void offerAt95Percent_shouldCounter() {
        Money offer = Money.of(new BigDecimal("95"));
        OfferDecision decision = engine.evaluate(offer, FLOOR_PRICE, CURRENT_PRICE, 0);

        assertThat(decision.type()).isEqualTo(OfferDecision.DecisionType.COUNTER);
        assertThat(decision.acceptedPrice()).isNull();
        assertThat(decision.counterPrice()).isEqualByComparingTo("95.00");
    }

    @Test
    @DisplayName("出价等于底价×0.9 → COUNTER")
    void offerAt90Percent_shouldCounter() {
        Money offer = Money.of(new BigDecimal("90"));
        OfferDecision decision = engine.evaluate(offer, FLOOR_PRICE, CURRENT_PRICE, 0);

        assertThat(decision.type()).isEqualTo(OfferDecision.DecisionType.COUNTER);
        assertThat(decision.acceptedPrice()).isNull();
        assertThat(decision.counterPrice()).isEqualByComparingTo("95.00");
    }

    @Test
    @DisplayName("出价低于底价×0.9，正常阶梯 → REJECT")
    void offerBelow90Percent_normalLevel_shouldReject() {
        Money offer = Money.of(new BigDecimal("89"));
        OfferDecision decision = engine.evaluate(offer, FLOOR_PRICE, CURRENT_PRICE, 0);

        assertThat(decision.type()).isEqualTo(OfferDecision.DecisionType.REJECT);
        assertThat(decision.acceptedPrice()).isNull();
        assertThat(decision.counterPrice()).isNull();
        assertThat(decision.reason()).isEqualTo("出价过低");
    }

    @Test
    @DisplayName("出价低于底价×0.9，底价阶梯但低于 ×0.85 → REJECT")
    void offerBelow90Percent_level3_below85_shouldReject() {
        Money offer = Money.of(new BigDecimal("84"));
        OfferDecision decision = engine.evaluate(offer, FLOOR_PRICE, CURRENT_PRICE, 3);

        assertThat(decision.type()).isEqualTo(OfferDecision.DecisionType.REJECT);
        assertThat(decision.acceptedPrice()).isNull();
        assertThat(decision.counterPrice()).isNull();
    }

    @Test
    @DisplayName("出价高于底价×0.85但低于×0.9，底价阶梯 → COUNTER（最后一次还价，counterPrice = 底价）")
    void offerBetween85And90Percent_level3_shouldCounterLastChance() {
        Money offer = Money.of(new BigDecimal("89"));
        OfferDecision decision = engine.evaluate(offer, FLOOR_PRICE, CURRENT_PRICE, 3);

        assertThat(decision.type()).isEqualTo(OfferDecision.DecisionType.COUNTER);
        assertThat(decision.acceptedPrice()).isNull();
        assertThat(decision.counterPrice()).isEqualByComparingTo("100");
        assertThat(decision.reason()).isEqualTo("商品已到底价，最后一次还价至底价");
    }

    @Test
    @DisplayName("出价等于底价×0.85，底价阶梯 → COUNTER（counterPrice = 底价）")
    void offerAt85Percent_level3_shouldCounterLastChance() {
        Money offer = Money.of(new BigDecimal("85"));
        OfferDecision decision = engine.evaluate(offer, FLOOR_PRICE, CURRENT_PRICE, 3);

        assertThat(decision.type()).isEqualTo(OfferDecision.DecisionType.COUNTER);
        assertThat(decision.acceptedPrice()).isNull();
        assertThat(decision.counterPrice()).isEqualByComparingTo("100");
        assertThat(decision.reason()).isEqualTo("商品已到底价，最后一次还价至底价");
    }

    @Test
    @DisplayName("出价等于底价×0.85，正常阶梯 → REJECT")
    void offerAt85Percent_normalLevel_shouldReject() {
        Money offer = Money.of(new BigDecimal("85"));
        OfferDecision decision = engine.evaluate(offer, FLOOR_PRICE, CURRENT_PRICE, 0);

        assertThat(decision.type()).isEqualTo(OfferDecision.DecisionType.REJECT);
        assertThat(decision.acceptedPrice()).isNull();
        assertThat(decision.counterPrice()).isNull();
    }

    @Test
    @DisplayName("出价过低（底价×0.5）→ REJECT")
    void offerTooLow_shouldReject() {
        Money offer = Money.of(new BigDecimal("50"));
        OfferDecision decision = engine.evaluate(offer, FLOOR_PRICE, CURRENT_PRICE, 0);

        assertThat(decision.type()).isEqualTo(OfferDecision.DecisionType.REJECT);
        assertThat(decision.acceptedPrice()).isNull();
        assertThat(decision.counterPrice()).isNull();
    }

    @Test
    @DisplayName("出价为0 → REJECT")
    void zeroOffer_shouldReject() {
        Money offer = Money.of(new BigDecimal("0"));
        OfferDecision decision = engine.evaluate(offer, FLOOR_PRICE, CURRENT_PRICE, 0);

        assertThat(decision.type()).isEqualTo(OfferDecision.DecisionType.REJECT);
        assertThat(decision.acceptedPrice()).isNull();
        assertThat(decision.counterPrice()).isNull();
    }
}
