package com.cartethyia.easyorange.favorite.repository;

import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import com.cartethyia.easyorange.favorite.domain.port.output.ProductInfoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.cartethyia.easyorange.favorite.FavoriteTestApplication.class)
@Testcontainers
@Tag("integration")
@DisplayName("FavoriteRepository 集成测试")
class FavoriteRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>(
        DockerImageName.parse("mysql:8.0")
    )
        .withDatabaseName("easyorange_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @MockitoBean
    private ProductInfoPort productInfoPort;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Long USER_ID = 9001L;
    private static final Long PRODUCT_ID_1 = 10001L;
    private static final Long PRODUCT_ID_2 = 10002L;
    private static final Long PRODUCT_ID_3 = 10003L;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM eo_favorite WHERE user_id IN (" + USER_ID + ")");
    }

    @Nested
    @DisplayName("保存和查询测试")
    class SaveAndFindTests {

        @Test
        @DisplayName("保存收藏成功")
        void save_shouldPersistFavorite() {
            Favorite favorite = Favorite.create(USER_ID, PRODUCT_ID_1);
            Favorite saved = favoriteRepository.save(favorite);

            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getProductId()).isEqualTo(PRODUCT_ID_1);
            assertThat(saved.getCreateTime()).isNotNull();
        }

        @Test
        @DisplayName("根据 ID 查询收藏")
        void findById_shouldReturnFavorite() {
            Favorite saved = favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_1));

            Optional<Favorite> found = favoriteRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getProductId()).isEqualTo(PRODUCT_ID_1);
        }

        @Test
        @DisplayName("根据 ID 查询不存在的收藏返回空")
        void findById_nonExistent_returnsEmpty() {
            Optional<Favorite> found = favoriteRepository.findById(999999L);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("根据用户 ID 和商品 ID 查询收藏")
        void findByUserIdAndProductId_shouldReturnFavorite() {
            favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_1));

            Optional<Favorite> found = favoriteRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID_1);

            assertThat(found).isPresent();
            assertThat(found.get().getProductId()).isEqualTo(PRODUCT_ID_1);
        }

        @Test
        @DisplayName("根据用户 ID 分页查询收藏")
        void findByUserId_shouldReturnPagedFavorites() {
            favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_1));
            favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_2));
            favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_3));

            List<Favorite> favorites = favoriteRepository.findByUserId(USER_ID, 0, 10);

            assertThat(favorites).hasSize(3);
        }

        @Test
        @DisplayName("统计用户收藏数量")
        void countByUserId_shouldReturnCorrectCount() {
            favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_1));
            favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_2));

            long count = favoriteRepository.countByUserId(USER_ID);

            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("批量查询测试")
    class BatchQueryTests {

        @Test
        @DisplayName("批量检查收藏状态")
        void findFavoritedProductIds_shouldReturnFavoritedIds() {
            favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_1));
            favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_3));

            Set<Long> favoritedIds = favoriteRepository.findFavoritedProductIds(
                    USER_ID, List.of(PRODUCT_ID_1, PRODUCT_ID_2, PRODUCT_ID_3));

            assertThat(favoritedIds).containsExactlyInAnyOrder(PRODUCT_ID_1, PRODUCT_ID_3);
        }

        @Test
        @DisplayName("批量检查 - 全部未收藏")
        void findFavoritedProductIds_noneFavorited_returnsEmptySet() {
            Set<Long> favoritedIds = favoriteRepository.findFavoritedProductIds(
                    USER_ID, List.of(PRODUCT_ID_1, PRODUCT_ID_2));

            assertThat(favoritedIds).isEmpty();
        }

        @Test
        @DisplayName("检查单个商品是否已收藏")
        void existsByUserIdAndProductId_shouldReturnTrue() {
            favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_1));

            boolean exists = favoriteRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID_1);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("检查单个商品是否已收藏 - 未收藏")
        void existsByUserIdAndProductId_shouldReturnFalse() {
            boolean exists = favoriteRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID_1);

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("删除和软删除测试")
    class DeleteTests {

        @Test
        @DisplayName("软删除收藏")
        void removeById_shouldSoftDelete() {
            Favorite saved = favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_1));
            favoriteRepository.removeById(saved.getId());

            Optional<Favorite> found = favoriteRepository.findById(saved.getId());
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("批量软删除收藏")
        void removeByIds_shouldSoftDeleteMultiple() {
            Favorite saved1 = favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_1));
            Favorite saved2 = favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_2));

            favoriteRepository.removeByIds(List.of(saved1.getId(), saved2.getId()));

            assertThat(favoriteRepository.findById(saved1.getId())).isEmpty();
            assertThat(favoriteRepository.findById(saved2.getId())).isEmpty();
        }

        @Test
        @DisplayName("取消收藏后重新收藏 - revive 逻辑")
        void save_afterSoftDelete_shouldRevive() {
            Favorite saved = favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_1));
            favoriteRepository.removeById(saved.getId());

            Favorite revived = favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_1));

            assertThat(revived).isNotNull();
            assertThat(revived.getProductId()).isEqualTo(PRODUCT_ID_1);

            Optional<Favorite> found = favoriteRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID_1);
            assertThat(found).isPresent();
        }
    }

    @Nested
    @DisplayName("批量 ID 查询测试")
    class FindByIdsTests {

        @Test
        @DisplayName("根据 ID 列表查询收藏")
        void findByIds_shouldReturnMatchingFavorites() {
            Favorite saved1 = favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_1));
            Favorite saved2 = favoriteRepository.save(Favorite.create(USER_ID, PRODUCT_ID_2));

            List<Favorite> found = favoriteRepository.findByIds(List.of(saved1.getId(), saved2.getId()));

            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("根据空 ID 列表查询返回空列表")
        void findByIds_emptyList_returnsEmptyList() {
            List<Favorite> found = favoriteRepository.findByIds(List.of());

            assertThat(found).isEmpty();
        }
    }
}
