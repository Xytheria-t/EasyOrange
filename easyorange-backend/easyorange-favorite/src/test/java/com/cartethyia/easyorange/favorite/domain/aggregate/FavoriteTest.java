package com.cartethyia.easyorange.favorite.domain.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Favorite 聚合根单元测试")
class FavoriteTest {

    private static final String USER_ID = "user-1";
    private static final String PRODUCT_ID = "product-1";
    private static final String FAVORITE_ID = "fav-1";
    private static final BigDecimal PRICE = new BigDecimal("99.90");

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("正常创建收藏 — id 为 null，createTime 自动设置")
        void create_validParams_returnsNewFavorite() {
            var before = LocalDateTime.now().minusSeconds(1);

            Favorite favorite = Favorite.create(new FavoriteCreateSpec(USER_ID, PRODUCT_ID, PRICE));

            assertThat(favorite.userId()).isEqualTo(USER_ID);
            assertThat(favorite.productId()).isEqualTo(PRODUCT_ID);
            assertThat(favorite.id()).isNull();
            assertThat(favorite.createTime()).isNotNull();
            assertThat(favorite.createTime()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("userId 为 null 时抛 IllegalArgumentException")
        void create_nullUserId_throws() {
            assertThatThrownBy(() -> Favorite.create(new FavoriteCreateSpec(null, PRODUCT_ID, PRICE)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("userId 为空字符串时抛 BusinessException")
        void create_emptyUserId_throws() {
            assertThatThrownBy(() -> Favorite.create(new FavoriteCreateSpec("", PRODUCT_ID, PRICE)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("productId 为 null 时抛 BusinessException")
        void create_nullProductId_throws() {
            assertThatThrownBy(() -> Favorite.create(new FavoriteCreateSpec(USER_ID, null, PRICE)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("productId 为空字符串时抛 BusinessException")
        void create_emptyProductId_throws() {
            assertThatThrownBy(() -> Favorite.create(new FavoriteCreateSpec(USER_ID, "", PRICE)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("productId");
        }

        @Test
        @DisplayName("price 为 null 时抛 BusinessException")
        void create_nullPrice_throws() {
            assertThatThrownBy(() -> Favorite.create(new FavoriteCreateSpec(USER_ID, PRODUCT_ID, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("价格快照");
        }

        @Test
        @DisplayName("price 为负数时抛 BusinessException")
        void create_negativePrice_throws() {
            assertThatThrownBy(() -> Favorite.create(new FavoriteCreateSpec(USER_ID, PRODUCT_ID, new BigDecimal("-1"))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("价格快照");
        }

        @Test
        @DisplayName("正常创建时记录价格快照")
        void create_validParams_recordsPriceSnapshot() {
            Favorite favorite = Favorite.create(new FavoriteCreateSpec(USER_ID, PRODUCT_ID, PRICE));

            assertThat(favorite.priceSnapshot()).isEqualTo(PRICE);
        }
    }

    @Nested
    @DisplayName("isPriceDrop")
    class PriceDropTests {

        @Test
        @DisplayName("新价低于快照价时判定降价")
        void isPriceDrop_lowerPrice_returnsTrue() {
            Favorite favorite = Favorite.reconstitute(FAVORITE_ID, USER_ID, PRODUCT_ID, new BigDecimal("100.00"), null);

            assertThat(favorite.isPriceDrop(new BigDecimal("80.00"))).isTrue();
        }

        @Test
        @DisplayName("新价等于快照价时不算降价")
        void isPriceDrop_samePrice_returnsFalse() {
            Favorite favorite = Favorite.reconstitute(FAVORITE_ID, USER_ID, PRODUCT_ID, new BigDecimal("100.00"), null);

            assertThat(favorite.isPriceDrop(new BigDecimal("100.00"))).isFalse();
        }

        @Test
        @DisplayName("新价高于快照价时不算降价")
        void isPriceDrop_higherPrice_returnsFalse() {
            Favorite favorite = Favorite.reconstitute(FAVORITE_ID, USER_ID, PRODUCT_ID, new BigDecimal("100.00"), null);

            assertThat(favorite.isPriceDrop(new BigDecimal("120.00"))).isFalse();
        }

        @Test
        @DisplayName("无快照（存量数据未回填）时不算降价")
        void isPriceDrop_nullSnapshot_returnsFalse() {
            Favorite favorite = Favorite.reconstitute(FAVORITE_ID, USER_ID, PRODUCT_ID, null, null);

            assertThat(favorite.isPriceDrop(new BigDecimal("80.00"))).isFalse();
        }

        @Test
        @DisplayName("withPriceSnapshot 更新快照为最新价")
        void withPriceSnapshot_updatesSnapshot() {
            Favorite favorite = Favorite.reconstitute(FAVORITE_ID, USER_ID, PRODUCT_ID, new BigDecimal("100.00"), null);

            Favorite updated = favorite.withPriceSnapshot(new BigDecimal("80.00"));

            assertThat(updated.priceSnapshot()).isEqualTo(new BigDecimal("80.00"));
            assertThat(updated.id()).isEqualTo(favorite.id());
            assertThat(updated.userId()).isEqualTo(favorite.userId());
        }
    }

    @Nested
    @DisplayName("reconstitute")
    class ReconstituteTests {

        @Test
        @DisplayName("从持久层正确重建 — 保留 id 和 createTime")
        void reconstitute_validParams_returnsFavoriteWithId() {
            var createTime = LocalDateTime.of(2026, 1, 1, 12, 0);

            Favorite favorite = Favorite.reconstitute(FAVORITE_ID, USER_ID, PRODUCT_ID, PRICE, createTime);

            assertThat(favorite.id()).isEqualTo(FAVORITE_ID);
            assertThat(favorite.userId()).isEqualTo(USER_ID);
            assertThat(favorite.productId()).isEqualTo(PRODUCT_ID);
            assertThat(favorite.createTime()).isEqualTo(createTime);
        }
    }

    @Nested
    @DisplayName("validateOwnership")
    class ValidateOwnershipTests {

        @Test
        @DisplayName("归属正确时不抛异常")
        void validateOwnership_sameUser_doesNotThrow() {
            Favorite favorite = Favorite.create(new FavoriteCreateSpec(USER_ID, PRODUCT_ID, PRICE));

            assertThatCode(() -> favorite.validateOwnership(USER_ID)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("归属不正确时抛 BusinessException")
        void validateOwnership_differentUser_throwsBusinessException() {
            Favorite favorite = Favorite.create(new FavoriteCreateSpec(USER_ID, PRODUCT_ID, PRICE));

            assertThatThrownBy(() -> favorite.validateOwnership("other-user"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权操作");
        }
    }
}
