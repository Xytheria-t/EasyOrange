package com.cartethyia.easyorange.product.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ProductAction 状态机单元测试")
class ProductActionTest {

    static Stream<Arguments> actionSourceCases() {
        return Stream.of(
                Arguments.of(ProductAction.SUBMIT_FOR_REVIEW, Set.of(ProductStatus.DRAFT, ProductStatus.REJECTED)),
                Arguments.of(ProductAction.PUT_ONLINE, Set.of(ProductStatus.DRAFT, ProductStatus.OFFLINE)),
                Arguments.of(ProductAction.APPROVE, Set.of(ProductStatus.PENDING_REVIEW)),
                Arguments.of(ProductAction.REJECT, Set.of(ProductStatus.PENDING_REVIEW)),
                Arguments.of(ProductAction.TAKE_OFFLINE, Set.of(ProductStatus.ONLINE)),
                Arguments.of(ProductAction.MARK_AS_SOLD, Set.of(ProductStatus.ONLINE)));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("actionSourceCases")
    @DisplayName("canApply 仅允许声明的前置状态")
    void canApply_onlyAllowsDeclaredSources(ProductAction action, Set<ProductStatus> expectedSources) {
        for (ProductStatus status : ProductStatus.values()) {
            boolean expected = expectedSources.contains(status);
            assertThat(action.canApply(status))
                    .as("%s from %s", action, status)
                    .isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("动作目标状态声明正确")
    void actionMetadata_targets() {
        assertThat(ProductAction.SUBMIT_FOR_REVIEW.target()).isEqualTo(ProductStatus.PENDING_REVIEW);
        assertThat(ProductAction.PUT_ONLINE.target()).isEqualTo(ProductStatus.ONLINE);
        assertThat(ProductAction.APPROVE.target()).isEqualTo(ProductStatus.ONLINE);
        assertThat(ProductAction.REJECT.target()).isEqualTo(ProductStatus.REJECTED);
        assertThat(ProductAction.TAKE_OFFLINE.target()).isEqualTo(ProductStatus.OFFLINE);
        assertThat(ProductAction.MARK_AS_SOLD.target()).isEqualTo(ProductStatus.SOLD);
    }

    @Test
    @DisplayName("终端状态无任何动作可触发")
    void canApply_terminalStatesHaveNoActions() {
        for (ProductAction action : ProductAction.values()) {
            assertThat(action.canApply(ProductStatus.SOLD))
                    .as("%s from terminal %s", action, ProductStatus.SOLD)
                    .isFalse();
        }
    }
}
