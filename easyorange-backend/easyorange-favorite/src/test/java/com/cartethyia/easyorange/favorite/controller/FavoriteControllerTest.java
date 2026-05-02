package com.cartethyia.easyorange.favorite.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.favorite.controller.request.BatchRemoveRequest;
import com.cartethyia.easyorange.favorite.service.FavoriteService;
import com.cartethyia.easyorange.favorite.service.dto.AddFavoriteDTO;
import com.cartethyia.easyorange.favorite.service.dto.FavoritePageQuery;
import com.cartethyia.easyorange.favorite.service.dto.FavoriteVO;
import com.cartethyia.easyorange.favorite.service.dto.RemoveFavoriteDTO;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("收藏控制器测试")
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    @InjectMocks
    private FavoriteController favoriteController;

    @Test
    @DisplayName("获取收藏列表成功")
    void testGetFavorites() {
        FavoriteVO favoriteVO = FavoriteVO.builder()
                .id(1L)
                .productId(2001L)
                .product(ProductVO.builder()
                        .id(2001L)
                        .title("测试商品")
                        .price(new BigDecimal("99.99"))
                        .build())
                .build();

        PageResult<FavoriteVO> pageResult = PageResult.of(
                List.of(favoriteVO), 1L, 1, 10
        );

        when(favoriteService.queryFavorites(any(FavoritePageQuery.class))).thenReturn(pageResult);

        var result = favoriteController.getFavorites(1, 10);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data().total()).isEqualTo(1L);
        assertThat(result.data().records().get(0).getId()).isEqualTo(1L);
        assertThat(result.data().records().get(0).getProductId()).isEqualTo(2001L);
        assertThat(result.data().records().get(0).getProduct().getTitle()).isEqualTo("测试商品");
        verify(favoriteService).queryFavorites(any(FavoritePageQuery.class));
    }

    @Test
    @DisplayName("添加收藏成功")
    void testAddFavorite() {
        doNothing().when(favoriteService).addFavorite(any(AddFavoriteDTO.class));

        var result = favoriteController.addFavorite(2001L);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(favoriteService).addFavorite(any(AddFavoriteDTO.class));
    }

    @Test
    @DisplayName("移除收藏成功")
    void testRemoveFavorite() {
        doNothing().when(favoriteService).removeFavorite(any(RemoveFavoriteDTO.class));

        var result = favoriteController.removeFavorite(2001L);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(favoriteService).removeFavorite(any(RemoveFavoriteDTO.class));
    }

    @Test
    @DisplayName("批量移除收藏成功")
    void testRemoveManyFavorites() {
        BatchRemoveRequest request = new BatchRemoveRequest();
        request.setIds(List.of(1L, 2L, 3L));
        doNothing().when(favoriteService).removeManyFavorites(any());

        var result = favoriteController.removeManyFavorites(request);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(favoriteService).removeManyFavorites(List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("检查是否收藏 - 已收藏")
    void testCheckIsFavorited_True() {
        when(favoriteService.isFavorited(2001L)).thenReturn(true);

        var result = favoriteController.checkIsFavorited(2001L);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data()).isTrue();
    }

    @Test
    @DisplayName("检查是否收藏 - 未收藏")
    void testCheckIsFavorited_False() {
        when(favoriteService.isFavorited(2001L)).thenReturn(false);

        var result = favoriteController.checkIsFavorited(2001L);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data()).isFalse();
    }

    @Test
    @DisplayName("获取收藏数量")
    void testGetFavoriteCount() {
        when(favoriteService.getFavoriteCount()).thenReturn(5L);

        var result = favoriteController.getFavoriteCount();

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data()).isEqualTo(5L);
    }
}
