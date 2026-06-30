package com.cartethyia.easyorange.favorite.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.assembler.FavoriteAssembler;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.controller.FavoriteController;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request.BatchCheckRequest;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request.BatchRemoveRequest;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.response.FavoriteResponse;
import com.cartethyia.easyorange.favorite.application.service.FavoriteService;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("收藏控制器测试")
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private FavoriteAssembler favoriteAssembler;

    @InjectMocks
    private FavoriteController favoriteController;

    @Test
    @DisplayName("获取收藏列表成功")
    void testGetFavorites() {
        Favorite favorite = Favorite.reconstitute("1", "1001", "2001", null);
        PageResult<Favorite> pageResult = PageResult.of(List.of(favorite), 1L, 1, 10);

        FavoriteResponse favoriteResponse = FavoriteResponse.builder()
                .id("1").productId("2001").build();
        PageResult<FavoriteResponse> responsePageResult = PageResult.of(
                List.of(favoriteResponse), 1L, 1, 10);

        when(favoriteService.queryFavorites(1, 10)).thenReturn(pageResult);
        when(favoriteAssembler.toPageResult(pageResult, 1, 10)).thenReturn(responsePageResult);

        var result = favoriteController.getFavorites(1, 10);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data().total()).isEqualTo(1L);
        assertThat(result.data().records().get(0).getId()).isEqualTo("1");
        assertThat(result.data().records().get(0).getProductId()).isEqualTo("2001");
        verify(favoriteService).queryFavorites(1, 10);
    }

    @Test
    @DisplayName("添加收藏成功")
    void testAddFavorite() {
        doNothing().when(favoriteService).addFavorite(anyString());

        var result = favoriteController.addFavorite("2001");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(favoriteService).addFavorite("2001");
    }

    @Test
    @DisplayName("移除收藏成功")
    void testRemoveFavorite() {
        doNothing().when(favoriteService).removeFavorite(anyString());

        var result = favoriteController.removeFavorite("2001");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(favoriteService).removeFavorite("2001");
    }

    @Test
    @DisplayName("批量移除收藏成功")
    void testRemoveManyFavorites() {
        BatchRemoveRequest request = new BatchRemoveRequest();
        request.setIds(List.of("1", "2", "3"));
        doNothing().when(favoriteService).removeManyFavorites(any());

        var result = favoriteController.removeManyFavorites(request);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(favoriteService).removeManyFavorites(List.of("1", "2", "3"));
    }

    @Test
    @DisplayName("检查是否收藏 - 已收藏")
    void testCheckIsFavorited_True() {
        when(favoriteService.isFavorited("2001")).thenReturn(true);

        var result = favoriteController.checkIsFavorited("2001");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data()).isTrue();
    }

    @Test
    @DisplayName("检查是否收藏 - 未收藏")
    void testCheckIsFavorited_False() {
        when(favoriteService.isFavorited("2001")).thenReturn(false);

        var result = favoriteController.checkIsFavorited("2001");

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

    @Test
    @DisplayName("批量检查收藏状态")
    void testBatchCheckFavorited() {
        Map<String, Boolean> checkResult = Map.of("2001", true, "2002", false);
        when(favoriteService.batchCheckFavorited(List.of("2001", "2002"))).thenReturn(checkResult);

        BatchCheckRequest request = new BatchCheckRequest();
        request.setProductIds(List.of("2001", "2002"));
        var result = favoriteController.batchCheckFavorited(request);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data()).hasSize(2);
        assertThat(result.data().get("2001")).isTrue();
        assertThat(result.data().get("2002")).isFalse();
    }
}