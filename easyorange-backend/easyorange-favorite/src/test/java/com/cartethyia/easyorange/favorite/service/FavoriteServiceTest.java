package com.cartethyia.easyorange.favorite.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import com.cartethyia.easyorange.favorite.domain.repository.FavoriteRepository;
import com.cartethyia.easyorange.favorite.infrastructure.acl.ProductAclService;
import com.cartethyia.easyorange.favorite.service.dto.AddFavoriteDTO;
import com.cartethyia.easyorange.favorite.service.dto.FavoritePageQuery;
import com.cartethyia.easyorange.favorite.service.dto.FavoriteVO;
import com.cartethyia.easyorange.favorite.service.dto.RemoveFavoriteDTO;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("收藏服务测试")
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ProductAclService productAclService;

    private FavoriteService favoriteService;

    private MockedStatic<SecurityContextUtil> securityContextMock;
    private static final Long TEST_USER_ID = 1001L;
    private static final Long TEST_PRODUCT_ID = 2001L;

    @BeforeEach
    void setUp() {
        securityContextMock = mockStatic(SecurityContextUtil.class);
        securityContextMock.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(TEST_USER_ID);

        favoriteService = new FavoriteService(favoriteRepository, productAclService);
    }

    @AfterEach
    void tearDown() {
        securityContextMock.close();
    }

    @Test
    @DisplayName("添加收藏成功")
    void addFavorite_success() {
        when(productAclService.productExists(TEST_PRODUCT_ID)).thenReturn(true);
        when(productAclService.isOwnProduct(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(false);
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(false);
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(inv -> inv.getArgument(0));

        favoriteService.addFavorite(AddFavoriteDTO.builder().productId(TEST_PRODUCT_ID).build());

        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("添加收藏 - 商品不存在时抛出异常")
    void addFavorite_productNotFound() {
        when(productAclService.productExists(TEST_PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> favoriteService.addFavorite(AddFavoriteDTO.builder().productId(TEST_PRODUCT_ID).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("商品不存在");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("添加收藏 - 已收藏时抛出异常")
    void addFavorite_alreadyFavorited() {
        when(productAclService.productExists(TEST_PRODUCT_ID)).thenReturn(true);
        when(productAclService.isOwnProduct(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(false);
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(AddFavoriteDTO.builder().productId(TEST_PRODUCT_ID).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("已收藏过该商品");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("添加收藏 - 不能收藏自己的商品")
    void addFavorite_cannotFavoriteOwnProduct() {
        when(productAclService.productExists(TEST_PRODUCT_ID)).thenReturn(true);
        when(productAclService.isOwnProduct(TEST_USER_ID, TEST_PRODUCT_ID)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(AddFavoriteDTO.builder().productId(TEST_PRODUCT_ID).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能收藏自己的商品");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("移除收藏成功")
    void removeFavorite_success() {
        Favorite favorite = Favorite.reconstitute(1L, TEST_USER_ID, TEST_PRODUCT_ID, null);
        when(favoriteRepository.findByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
                .thenReturn(Optional.of(favorite));

        favoriteService.removeFavorite(RemoveFavoriteDTO.builder().productId(TEST_PRODUCT_ID).build());

        verify(favoriteRepository).removeById(1L);
    }

    @Test
    @DisplayName("移除收藏 - 未收藏时抛出异常")
    void removeFavorite_notFavorited() {
        when(favoriteRepository.findByUserIdAndProductId(TEST_USER_ID, TEST_PRODUCT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.removeFavorite(RemoveFavoriteDTO.builder().productId(TEST_PRODUCT_ID).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("未收藏过该商品");

        verify(favoriteRepository, never()).removeById(any());
    }

    @Test
    @DisplayName("批量移除收藏成功")
    void removeManyFavorites_success() {
        List<Long> favoriteIds = List.of(1L, 2L, 3L);
        Favorite favorite1 = Favorite.reconstitute(1L, TEST_USER_ID, 2001L, null);
        Favorite favorite2 = Favorite.reconstitute(2L, TEST_USER_ID, 2002L, null);
        Favorite favorite3 = Favorite.reconstitute(3L, TEST_USER_ID, 2003L, null);
        
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
        List<Long> favoriteIds = List.of(1L, 2L);
        Favorite ownFavorite = Favorite.reconstitute(1L, TEST_USER_ID, 2001L, null);
        Favorite otherUserFavorite = Favorite.reconstitute(2L, 9999L, 2002L, null);
        
        when(favoriteRepository.findByIds(favoriteIds)).thenReturn(List.of(ownFavorite, otherUserFavorite));

        assertThatThrownBy(() -> favoriteService.removeManyFavorites(favoriteIds))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权操作他人的收藏");

        verify(favoriteRepository, never()).removeByIds(any());
    }

    @Test
    @DisplayName("查询用户收藏列表")
    void queryFavorites_success() {
        FavoritePageQuery query = FavoritePageQuery.builder().pageNum(1).pageSize(10).build();

        Favorite favorite1 = Favorite.reconstitute(1L, TEST_USER_ID, 2001L, null);
        Favorite favorite2 = Favorite.reconstitute(2L, TEST_USER_ID, 2002L, null);

        ProductReadModel product1 = new ProductReadModel(2001L, 3001L, "user1", "avatar1",
                2L, "分类", "商品1", "描述", new BigDecimal("99.99"), null,
                10, 1, "上架", 100, 1, "全新",
                "北京", "微信", List.of("http://img/1.jpg"), "http://img/1.jpg",
                null, null);
        ProductReadModel product2 = new ProductReadModel(2002L, 3002L, "user2", "avatar2",
                2L, "分类", "商品2", "描述", new BigDecimal("199.99"), null,
                5, 1, "上架", 50, 2, "几乎全新",
                "上海", "微信", List.of("http://img/2.jpg"), "http://img/2.jpg",
                null, null);

        ProductVO vo1 = ProductVO.builder().id(2001L).title("商品1").price(new BigDecimal("99.99")).build();
        ProductVO vo2 = ProductVO.builder().id(2002L).title("商品2").price(new BigDecimal("199.99")).build();

        when(favoriteRepository.countByUserId(TEST_USER_ID)).thenReturn(2L);
        when(favoriteRepository.findByUserId(eq(TEST_USER_ID), eq(0L), eq(10L)))
                .thenReturn(List.of(favorite1, favorite2));
        when(productAclService.findProductsByIds(List.of(2001L, 2002L)))
                .thenReturn(List.of(product1, product2));
        when(productAclService.findSellersByIds(any())).thenReturn(Collections.emptyMap());
        when(productAclService.assembleProductVOs(any(), any()))
                .thenReturn(List.of(vo1, vo2));

        PageResult<FavoriteVO> result = favoriteService.queryFavorites(query);

        assertThat(result).isNotNull();
        assertThat(result.total()).isEqualTo(2L);
        assertThat(result.records()).hasSize(2);
        assertThat(result.records().get(0).getId()).isEqualTo(1L);
        assertThat(result.records().get(0).getProductId()).isEqualTo(2001L);
        assertThat(result.records().get(0).getProduct()).isNotNull();
        assertThat(result.records().get(0).getProduct().getTitle()).isEqualTo("商品1");
        assertThat(result.records().get(1).getId()).isEqualTo(2L);
        assertThat(result.records().get(1).getProductId()).isEqualTo(2002L);
        assertThat(result.records().get(1).getProduct().getTitle()).isEqualTo("商品2");
    }

    @Test
    @DisplayName("查询收藏列表 - 空列表")
    void queryFavorites_emptyList() {
        FavoritePageQuery query = FavoritePageQuery.builder().pageNum(1).pageSize(10).build();

        when(favoriteRepository.countByUserId(TEST_USER_ID)).thenReturn(0L);
        when(favoriteRepository.findByUserId(eq(TEST_USER_ID), eq(0L), eq(10L)))
                .thenReturn(List.of());

        PageResult<FavoriteVO> result = favoriteService.queryFavorites(query);

        assertThat(result).isNotNull();
        assertThat(result.total()).isEqualTo(0L);
        assertThat(result.records()).isEmpty();
    }

    @Test
    @DisplayName("检查用户是否收藏指定商品 - 已收藏")
    void isFavorited_true() {
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, 2001L)).thenReturn(true);

        boolean result = favoriteService.isFavorited(2001L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("检查用户是否收藏指定商品 - 未收藏")
    void isFavorited_false() {
        when(favoriteRepository.existsByUserIdAndProductId(TEST_USER_ID, 2001L)).thenReturn(false);

        boolean result = favoriteService.isFavorited(2001L);

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
        List<Long> productIds = List.of(2001L, 2002L, 2003L);
        when(favoriteRepository.findFavoritedProductIds(TEST_USER_ID, productIds))
                .thenReturn(Set.of(2001L, 2003L));

        Map<Long, Boolean> result = favoriteService.batchCheckFavorited(productIds);

        assertThat(result).hasSize(3);
        assertThat(result.get(2001L)).isTrue();
        assertThat(result.get(2002L)).isFalse();
        assertThat(result.get(2003L)).isTrue();
    }

    @Test
    @DisplayName("批量检查收藏状态 - 空列表")
    void batchCheckFavorited_emptyList() {
        Map<Long, Boolean> result = favoriteService.batchCheckFavorited(List.of());

        assertThat(result).isEmpty();
        verify(favoriteRepository, never()).findFavoritedProductIds(any(), any());
    }
}
