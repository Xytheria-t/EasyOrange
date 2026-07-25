package com.cartethyia.easyorange.favorite.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Favorite 聚合根单元测试")
class FavoriteTest {

    private static final String USER_ID = "user-1";
    private static final String PRODUCT_ID = "product-1";
    private static final String FAVORITE_ID = "fav-1";

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("正常创建收藏 — id 为 null，createTime 自动设置")
        void create_validParams_returnsNewFavorite() {
            var before = LocalDateTime.now().minusSeconds(1);

            Favorite favorite = Favorite.create(USER_ID, PRODUCT_ID);

            assertThat(favorite.getUserId()).isEqualTo(USER_ID);
            assertThat(favorite.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(favorite.getId()).isNull();
            assertThat(favorite.getCreateTime()).isNotNull();
            assertThat(favorite.getCreateTime()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("userId 为 null 时抛 IllegalArgumentException")
        void create_nullUserId_throws() {
            assertThatThrownBy(() -> Favorite.create(null, PRODUCT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("userId 为空字符串时不抛异常（现有行为：仅校验 null）")
        void create_emptyUserId_doesNotThrow() {
            assertThatCode(() -> Favorite.create("", PRODUCT_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("productId 为 null 时抛 IllegalArgumentException")
        void create_nullProductId_throws() {
            assertThatThrownBy(() -> Favorite.create(USER_ID, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("reconstitute")
    class ReconstituteTests {

        @Test
        @DisplayName("从持久层正确重建 — 保留 id 和 createTime")
        void reconstitute_validParams_returnsFavoriteWithId() {
            var createTime = LocalDateTime.of(2026, 1, 1, 12, 0);

            Favorite favorite = Favorite.reconstitute(FAVORITE_ID, USER_ID, PRODUCT_ID, createTime);

            assertThat(favorite.getId()).isEqualTo(FAVORITE_ID);
            assertThat(favorite.getUserId()).isEqualTo(USER_ID);
            assertThat(favorite.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(favorite.getCreateTime()).isEqualTo(createTime);
        }
    }

    @Nested
    @DisplayName("belongsTo")
    class BelongsToTests {

        @Test
        @DisplayName("userId 匹配时返回 true")
        void belongsTo_sameUser_returnsTrue() {
            Favorite favorite = Favorite.create(USER_ID, PRODUCT_ID);

            assertThat(favorite.belongsTo(USER_ID)).isTrue();
        }

        @Test
        @DisplayName("userId 不匹配时返回 false")
        void belongsTo_differentUser_returnsFalse() {
            Favorite favorite = Favorite.create(USER_ID, PRODUCT_ID);

            assertThat(favorite.belongsTo("other-user")).isFalse();
        }

        @Test
        @DisplayName("传入 null userId 时返回 false（Objects.equals 安全处理 null）")
        void belongsTo_nullUserId_returnsFalse() {
            Favorite favorite = Favorite.create(USER_ID, PRODUCT_ID);

            assertThat(favorite.belongsTo(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("validateOwnership")
    class ValidateOwnershipTests {

        @Test
        @DisplayName("归属正确时不抛异常")
        void validateOwnership_sameUser_doesNotThrow() {
            Favorite favorite = Favorite.create(USER_ID, PRODUCT_ID);

            assertThatCode(() -> favorite.validateOwnership(USER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("归属不正确时抛 BusinessException")
        void validateOwnership_differentUser_throwsBusinessException() {
            Favorite favorite = Favorite.create(USER_ID, PRODUCT_ID);

            assertThatThrownBy(() -> favorite.validateOwnership("other-user"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权操作");
        }
    }
}
