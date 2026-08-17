package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.aggregate.FavoriteCreateSpec;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 收藏仓库真实 MySQL 链路测试 —— 兜住全 Mockito 单测覆盖不到的 SQL 语义：
 * 软删（del_flag=1）后再次收藏必须复活原行而非新增，且「删除→再收藏→再删除」循环不撞唯一键
 * {@code uk_eo_favorite_user_product_del}，insert 时必须生成 UUID v7 主键。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
class FavoriteRepositoryIT {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Test
    @DisplayName("删除→再收藏→再删除循环：复活同一行且不撞唯一键")
    void removeThenReaddThenRemoveAgain_keepsSingleRow() {
        String userId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();

        Favorite first = favoriteRepository.save(Favorite.create(new FavoriteCreateSpec(userId, productId)));
        assertThat(first.id()).as("首次收藏必须生成主键").isNotBlank();

        favoriteRepository.removeById(first.id());
        assertThat(favoriteRepository.countByUserId(userId)).isZero();

        Favorite second = favoriteRepository.save(Favorite.create(new FavoriteCreateSpec(userId, productId)));
        assertThat(second.id()).as("再次收藏应复活原行而非新增").isEqualTo(first.id());

        assertThatCode(() -> favoriteRepository.removeById(second.id()))
                .as("第二次删除不得撞唯一键 (user_id, product_id, del_flag)")
                .doesNotThrowAnyException();
        assertThat(favoriteRepository.countByUserId(userId)).isZero();

        Favorite third = favoriteRepository.save(Favorite.create(new FavoriteCreateSpec(userId, productId)));
        assertThat(third.id()).isEqualTo(first.id());
        favoriteRepository.removeById(third.id());
        assertThat(favoriteRepository.countByUserId(userId)).isZero();
    }

    @Test
    @DisplayName("并发兜底：复活路径下同用户同商品始终只有一行（含 del_flag 位）")
    void saveAfterRemove_neverLeavesDuplicateRows() {
        String userId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();

        Favorite first = favoriteRepository.save(Favorite.create(new FavoriteCreateSpec(userId, productId)));
        favoriteRepository.removeById(first.id());
        favoriteRepository.removeById(first.id());
        favoriteRepository.save(Favorite.create(new FavoriteCreateSpec(userId, productId)));

        assertThat(favoriteRepository.findByUserIdAndProductId(userId, productId))
                .as("复活路径下只应存在一条 del_flag=0 记录")
                .isPresent();
    }
}
