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
import com.cartethyia.easyorange.favorite.domain.port.PriceDropNotificationPort;
import com.cartethyia.easyorange.favorite.domain.port.ProductInfoPort;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
@DisplayName("收藏服务测试")
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ProductInfoPort productInfoPort;

    @Mock
    private PriceDropNotificationPort priceDropNotificationPort;

    private FavoriteService favoriteService;

    private static final String TEST_USER_ID = "1001";
    private static final String TEST_PRODUCT_ID = "2001";
    private static final BigDecimal TEST_PRICE = new BigDecimal("99.90");

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, productInfoPort, priceDropNotificationPort);
    }

    @Test
    @DisplayName("添加收藏成功")
    void addFavorite_success() {
        when(productInfoPort.findPriceByProductId(TEST_PRODUCT_ID)).thenReturn(Optional.of(TEST_PRICE));
        when(productInfoPort.isOwnProduct(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(false);
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
                .thenReturn(false);
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(inv -> inv.getArgument(0));

        favoriteService.addFavorite(TEST_USER_ID, TEST_PRODUCT_ID);

        ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository).save(captor.capture());
        assertThat(captor.getValue().priceSnapshot()).isEqualTo(TEST_PRICE);
    }

    @Test
    @DisplayName("添加收藏 - 商品不存在时抛出异常")
    void addFavorite_productNotFound() {
        when(productInfoPort.findPriceByProductId(TEST_PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.addFavorite(TEST_USER_ID, TEST_PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("商品不存在");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("添加收藏 - 已收藏时幂等成功（不重复插入）")
    void addFavorite_alreadyFavorited() {
        when(productInfoPort.findPriceByProductId(TEST_PRODUCT_ID)).thenReturn(Optional.of(TEST_PRICE));
        when(productInfoPort.isOwnProduct(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(false);
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
                .thenReturn(true);

        favoriteService.addFavorite(TEST_USER_ID, TEST_PRODUCT_ID);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("添加收藏 - 并发重复收藏撞唯一键时幂等成功")
    void addFavorite_duplicateKeyRace() {
        when(productInfoPort.findPriceByProductId(TEST_PRODUCT_ID)).thenReturn(Optional.of(TEST_PRICE));
        when(productInfoPort.isOwnProduct(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(false);
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
                .thenReturn(false);
        when(favoriteRepository.save(any(Favorite.class))).thenThrow(new DuplicateKeyException("duplicate"));

        favoriteService.addFavorite(TEST_USER_ID, TEST_PRODUCT_ID);
    }

    @Test
    @DisplayName("添加收藏 - 不能收藏自己的商品")
    void addFavorite_cannotFavoriteOwnProduct() {
        when(productInfoPort.findPriceByProductId(TEST_PRODUCT_ID)).thenReturn(Optional.of(TEST_PRICE));
        when(productInfoPort.isOwnProduct(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(TEST_USER_ID, TEST_PRODUCT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能收藏自己的商品");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("移除收藏成功")
    void removeFavorite_success() {
        Favorite favorite = Favorite.reconstitute("1", TEST_USER_ID, TEST_PRODUCT_ID, TEST_PRICE, null);
        when(favoriteRepository.findByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
                .thenReturn(Optional.of(favorite));

        favoriteService.removeFavorite(TEST_USER_ID, TEST_PRODUCT_ID);

        verify(favoriteRepository).removeById("1");
    }

    @Test
    @DisplayName("移除收藏 - 未收藏时幂等成功（不报错）")
    void removeFavorite_notFavorited() {
        when(favoriteRepository.findByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
                .thenReturn(Optional.empty());

        favoriteService.removeFavorite(TEST_USER_ID, TEST_PRODUCT_ID);

        verify(favoriteRepository, never()).removeById(any());
    }

    @Test
    @DisplayName("批量移除收藏成功")
    void removeManyFavorites_success() {
        List<String> favoriteIds = List.of("1", "2", "3");
        Favorite favorite1 = Favorite.reconstitute("1", TEST_USER_ID, "2001", TEST_PRICE, null);
        Favorite favorite2 = Favorite.reconstitute("2", TEST_USER_ID, "2002", TEST_PRICE, null);
        Favorite favorite3 = Favorite.reconstitute("3", TEST_USER_ID, "2003", TEST_PRICE, null);

        when(favoriteRepository.findByIds(favoriteIds)).thenReturn(List.of(favorite1, favorite2, favorite3));
        when(favoriteRepository.removeByIds(favoriteIds)).thenReturn(3);

        favoriteService.removeManyFavorites(TEST_USER_ID, favoriteIds);

        verify(favoriteRepository).findByIds(favoriteIds);
        verify(favoriteRepository).removeByIds(favoriteIds);
    }

    @Test
    @DisplayName("批量移除收藏 - 空列表时不执行")
    void removeManyFavorites_emptyList() {
        favoriteService.removeManyFavorites(TEST_USER_ID, List.of());

        verify(favoriteRepository, never()).findByIds(any());
        verify(favoriteRepository, never()).removeByIds(any());
    }

    @Test
    @DisplayName("批量移除收藏 - 包含他人收藏时抛出异常")
    void removeManyFavorites_containsOtherUserFavorite() {
        List<String> favoriteIds = List.of("1", "2");
        Favorite ownFavorite = Favorite.reconstitute("1", TEST_USER_ID, "2001", TEST_PRICE, null);
        Favorite otherUserFavorite = Favorite.reconstitute("2", "9999", "2002", TEST_PRICE, null);

        when(favoriteRepository.findByIds(favoriteIds)).thenReturn(List.of(ownFavorite, otherUserFavorite));

        assertThatThrownBy(() -> favoriteService.removeManyFavorites(TEST_USER_ID, favoriteIds))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权操作他人的收藏");

        verify(favoriteRepository, never()).removeByIds(any());
    }

    @Test
    @DisplayName("查询用户收藏列表")
    void queryFavorites_success() {
        Favorite favorite1 = Favorite.reconstitute("1", TEST_USER_ID, "2001", TEST_PRICE, null);
        Favorite favorite2 = Favorite.reconstitute("2", TEST_USER_ID, "2002", TEST_PRICE, null);

        when(favoriteRepository.countByUserId(TEST_USER_ID)).thenReturn(2L);
        when(favoriteRepository.findByUserId(eq(TEST_USER_ID), eq(0L), eq(10L)))
                .thenReturn(List.of(favorite1, favorite2));

        PageResult<Favorite> result = favoriteService.queryFavorites(TEST_USER_ID, 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.total()).isEqualTo(2L);
        assertThat(result.records()).hasSize(2);
        assertThat(result.records().get(0).id()).isEqualTo("1");
        assertThat(result.records().get(0).productId()).isEqualTo("2001");
        assertThat(result.records().get(1).id()).isEqualTo("2");
        assertThat(result.records().get(1).productId()).isEqualTo("2002");
    }

    @Test
    @DisplayName("查询收藏列表 - 空列表")
    void queryFavorites_emptyList() {
        when(favoriteRepository.countByUserId(TEST_USER_ID)).thenReturn(0L);
        when(favoriteRepository.findByUserId(eq(TEST_USER_ID), eq(0L), eq(10L))).thenReturn(List.of());

        PageResult<Favorite> result = favoriteService.queryFavorites(TEST_USER_ID, 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.total()).isEqualTo(0L);
        assertThat(result.records()).isEmpty();
    }

    @Test
    @DisplayName("检查用户是否收藏指定商品 - 已收藏")
    void isFavorited_true() {
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, "2001"))
                .thenReturn(true);

        boolean result = favoriteService.isFavorited(TEST_USER_ID, "2001");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("检查用户是否收藏指定商品 - 未收藏")
    void isFavorited_false() {
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, "2001"))
                .thenReturn(false);

        boolean result = favoriteService.isFavorited(TEST_USER_ID, "2001");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("获取用户收藏数量")
    void getFavoriteCount() {
        when(favoriteRepository.countByUserId(TEST_USER_ID)).thenReturn(5L);

        long count = favoriteService.getFavoriteCount(TEST_USER_ID);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("批量检查收藏状态")
    void batchCheckFavorited_success() {
        List<String> productIds = List.of("2001", "2002", "2003");
        when(favoriteRepository.findFavoritedProductIds(TEST_USER_ID, productIds))
                .thenReturn(Set.of("2001", "2003"));

        Map<String, Boolean> result = favoriteService.batchCheckFavorited(TEST_USER_ID, productIds);

        assertThat(result).hasSize(3);
        assertThat(result.get("2001")).isTrue();
        assertThat(result.get("2002")).isFalse();
        assertThat(result.get("2003")).isTrue();
    }

    @Test
    @DisplayName("批量检查收藏状态 - 空列表")
    void batchCheckFavorited_emptyList() {
        Map<String, Boolean> result = favoriteService.batchCheckFavorited(TEST_USER_ID, List.of());

        assertThat(result).isEmpty();
        verify(favoriteRepository, never()).findFavoritedProductIds(any(), any());
    }

    @Test
    @DisplayName("降价提醒 - 新价低于快照时通知并更新快照")
    void processPriceDrop_lowerPrice_notifiesAndUpdatesSnapshot() {
        BigDecimal snapshot = new BigDecimal("100.00");
        BigDecimal newPrice = new BigDecimal("80.00");
        Favorite favorite = Favorite.reconstitute("1", TEST_USER_ID, "2001", snapshot, null);
        when(favoriteRepository.findByProductId("2001")).thenReturn(List.of(favorite));
        when(favoriteRepository.updatePriceSnapshot("1", snapshot, newPrice)).thenReturn(true);

        favoriteService.processPriceDrop("2001", "测试商品", newPrice);

        verify(priceDropNotificationPort).sendPriceDropNotification(TEST_USER_ID, "2001", "测试商品", snapshot, newPrice);
        verify(favoriteRepository).updatePriceSnapshot("1", snapshot, newPrice);
    }

    @Test
    @DisplayName("降价提醒 - 新价不低于快照时不通知也不更新")
    void processPriceDrop_noDrop_doesNotNotify() {
        Favorite favorite = Favorite.reconstitute("1", TEST_USER_ID, "2001", new BigDecimal("100.00"), null);
        when(favoriteRepository.findByProductId("2001")).thenReturn(List.of(favorite));

        favoriteService.processPriceDrop("2001", "测试商品", new BigDecimal("100.00"));
        favoriteService.processPriceDrop("2001", "测试商品", new BigDecimal("120.00"));

        verify(priceDropNotificationPort, never()).sendPriceDropNotification(any(), any(), any(), any(), any());
        verify(favoriteRepository, never()).updatePriceSnapshot(any(), any(), any());
    }

    @Test
    @DisplayName("降价提醒 - 快照为空（存量数据）时只回填不通知")
    void processPriceDrop_nullSnapshot_backfillsWithoutNotify() {
        Favorite favorite = Favorite.reconstitute("1", TEST_USER_ID, "2001", null, null);
        when(favoriteRepository.findByProductId("2001")).thenReturn(List.of(favorite));
        when(favoriteRepository.updatePriceSnapshot("1", null, new BigDecimal("80.00")))
                .thenReturn(true);

        favoriteService.processPriceDrop("2001", "测试商品", new BigDecimal("80.00"));

        verify(favoriteRepository).updatePriceSnapshot("1", null, new BigDecimal("80.00"));
        verify(priceDropNotificationPort, never()).sendPriceDropNotification(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("降价提醒 - 重复事件幂等：CAS 未命中时不重复通知")
    void processPriceDrop_duplicateEvent_casMissSkipsNotify() {
        Favorite favorite = Favorite.reconstitute("1", TEST_USER_ID, "2001", new BigDecimal("80.00"), null);
        when(favoriteRepository.findByProductId("2001")).thenReturn(List.of(favorite));
        when(favoriteRepository.updatePriceSnapshot("1", new BigDecimal("80.00"), new BigDecimal("70.00")))
                .thenReturn(false);

        favoriteService.processPriceDrop("2001", "测试商品", new BigDecimal("70.00"));

        verify(priceDropNotificationPort, never()).sendPriceDropNotification(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("降价提醒 - 无收藏时无操作")
    void processPriceDrop_noFavorites_noop() {
        when(favoriteRepository.findByProductId("2001")).thenReturn(List.of());

        favoriteService.processPriceDrop("2001", "测试商品", new BigDecimal("70.00"));

        verify(favoriteRepository, never()).updatePriceSnapshot(any(), any(), any());
        verify(priceDropNotificationPort, never()).sendPriceDropNotification(any(), any(), any(), any(), any());
    }
}
