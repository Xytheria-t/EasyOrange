package com.cartethyia.easyorange.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.favorite.application.service.FavoriteService;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.port.ProductInfoPort;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("收藏服务测试")
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ProductInfoPort productInfoPort;

    private FavoriteService favoriteService;

    private static final String TEST_USER_ID = "1001";
    private static final String TEST_PRODUCT_ID = "2001";

    @BeforeEach
    void setUp() {
        TestSecurityUtil.setSecurityContext(TEST_USER_ID);

        favoriteService = new FavoriteService(favoriteRepository, productInfoPort);
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtil.clearSecurityContext();
    }

    @Test
    @DisplayName("添加收藏成功")
    void addFavorite_success() {
        when(productInfoPort.productExists(TEST_PRODUCT_ID)).thenReturn(true);
        when(productInfoPort.isOwnProduct(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(false);
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
                .thenReturn(false);
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(inv -> inv.getArgument(0));

        favoriteService.addFavorite(TEST_PRODUCT_ID);

        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("添加收藏 - 商品不存在时抛出异常")
    void addFavorite_productNotFound() {
        when(productInfoPort.productExists(TEST_PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> favoriteService.addFavorite(TEST_PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("商品不存在");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("添加收藏 - 已收藏时抛出异常")
    void addFavorite_alreadyFavorited() {
        when(productInfoPort.productExists(TEST_PRODUCT_ID)).thenReturn(true);
        when(productInfoPort.isOwnProduct(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(false);
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(TEST_PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("已收藏过该商品");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("添加收藏 - 不能收藏自己的商品")
    void addFavorite_cannotFavoriteOwnProduct() {
        when(productInfoPort.productExists(TEST_PRODUCT_ID)).thenReturn(true);
        when(productInfoPort.isOwnProduct(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(TEST_PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能收藏自己的商品");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("移除收藏成功")
    void removeFavorite_success() {
        Favorite favorite = Favorite.reconstitute("1", TEST_USER_ID, TEST_PRODUCT_ID, null);
        when(favoriteRepository.findByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
                .thenReturn(Optional.of(favorite));

        favoriteService.removeFavorite(TEST_PRODUCT_ID);

        verify(favoriteRepository).removeById("1");
    }

    @Test
    @DisplayName("移除收藏 - 未收藏时抛出异常")
    void removeFavorite_notFavorited() {
        when(favoriteRepository.findByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.removeFavorite(TEST_PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("未收藏过该商品");

        verify(favoriteRepository, never()).removeById(any());
    }

    @Test
    @DisplayName("批量移除收藏成功")
    void removeManyFavorites_success() {
        List<String> favoriteIds = List.of("1", "2", "3");
        Favorite favorite1 = Favorite.reconstitute("1", TEST_USER_ID, "2001", null);
        Favorite favorite2 = Favorite.reconstitute("2", TEST_USER_ID, "2002", null);
        Favorite favorite3 = Favorite.reconstitute("3", TEST_USER_ID, "2003", null);

        when(favoriteRepository.findByIds(favoriteIds)).thenReturn(List.of(favorite1, favorite2, favorite3));
        when(favoriteRepository.removeByIds(favoriteIds)).thenReturn(3);

        favoriteService.removeManyFavorites(favoriteIds);

        verify(favoriteRepository).findByIds(favoriteIds);
        verify(favoriteRepository).removeByIds(favoriteIds);
    }

    @Test
    @DisplayName("批量移除收藏 - 空列表时不执行")
    void removeManyFavorites_emptyList() {
        favoriteService.removeManyFavorites(List.of());

        verify(favoriteRepository, never()).findByIds(any());
        verify(favoriteRepository, never()).removeByIds(any());
    }

    @Test
    @DisplayName("批量移除收藏 - 包含他人收藏时抛出异常")
    void removeManyFavorites_containsOtherUserFavorite() {
        List<String> favoriteIds = List.of("1", "2");
        Favorite ownFavorite = Favorite.reconstitute("1", TEST_USER_ID, "2001", null);
        Favorite otherUserFavorite = Favorite.reconstitute("2", "9999", "2002", null);

        when(favoriteRepository.findByIds(favoriteIds)).thenReturn(List.of(ownFavorite, otherUserFavorite));

        assertThatThrownBy(() -> favoriteService.removeManyFavorites(favoriteIds))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权操作他人的收藏");

        verify(favoriteRepository, never()).removeByIds(any());
    }

    @Test
    @DisplayName("查询用户收藏列表")
    void queryFavorites_success() {
        Favorite favorite1 = Favorite.reconstitute("1", TEST_USER_ID, "2001", null);
        Favorite favorite2 = Favorite.reconstitute("2", TEST_USER_ID, "2002", null);

        when(favoriteRepository.countByUserId(TEST_USER_ID)).thenReturn(2L);
        when(favoriteRepository.findByUserId(eq(TEST_USER_ID), eq(0L), eq(10L)))
                .thenReturn(List.of(favorite1, favorite2));

        PageResult<Favorite> result = favoriteService.queryFavorites(1, 10);

        assertThat(result).isNotNull();
        assertThat(result.total()).isEqualTo(2L);
        assertThat(result.records()).hasSize(2);
        assertThat(result.records().get(0).getId()).isEqualTo("1");
        assertThat(result.records().get(0).getProductId()).isEqualTo("2001");
        assertThat(result.records().get(1).getId()).isEqualTo("2");
        assertThat(result.records().get(1).getProductId()).isEqualTo("2002");
    }

    @Test
    @DisplayName("查询收藏列表 - 空列表")
    void queryFavorites_emptyList() {
        when(favoriteRepository.countByUserId(TEST_USER_ID)).thenReturn(0L);
        when(favoriteRepository.findByUserId(eq(TEST_USER_ID), eq(0L), eq(10L))).thenReturn(List.of());

        PageResult<Favorite> result = favoriteService.queryFavorites(1, 10);

        assertThat(result).isNotNull();
        assertThat(result.total()).isEqualTo(0L);
        assertThat(result.records()).isEmpty();
    }

    @Test
    @DisplayName("检查用户是否收藏指定商品 - 已收藏")
    void isFavorited_true() {
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, "2001"))
                .thenReturn(true);

        boolean result = favoriteService.isFavorited("2001");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("检查用户是否收藏指定商品 - 未收藏")
    void isFavorited_false() {
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, "2001"))
                .thenReturn(false);

        boolean result = favoriteService.isFavorited("2001");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("获取用户收藏数量")
    void getFavoriteCount() {
        when(favoriteRepository.countByUserId(TEST_USER_ID)).thenReturn(5L);

        long count = favoriteService.getFavoriteCount();

        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("批量检查收藏状态")
    void batchCheckFavorited_success() {
        List<String> productIds = List.of("2001", "2002", "2003");
        when(favoriteRepository.findFavoritedProductIds(TEST_USER_ID, productIds))
                .thenReturn(Set.of("2001", "2003"));

        Map<String, Boolean> result = favoriteService.batchCheckFavorited(productIds);

        assertThat(result).hasSize(3);
        assertThat(result.get("2001")).isTrue();
        assertThat(result.get("2002")).isFalse();
        assertThat(result.get("2003")).isTrue();
    }

    @Test
    @DisplayName("批量检查收藏状态 - 空列表")
    void batchCheckFavorited_emptyList() {
        Map<String, Boolean> result = favoriteService.batchCheckFavorited(List.of());

        assertThat(result).isEmpty();
        verify(favoriteRepository, never()).findFavoritedProductIds(any(), any());
    }
}
