package com.cartethyia.easyorange.favorite.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.assembler.FavoriteAssembler;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.controller.FavoriteController;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request.BatchCheckRequest;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.request.BatchRemoveRequest;
import com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.response.FavoriteResponse;
import com.cartethyia.easyorange.favorite.application.service.FavoriteService;
import com.cartethyia.easyorange.favorite.domain.aggregate.Favorite;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("收藏控制器测试")
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private FavoriteAssembler favoriteAssembler;

    @InjectMocks
    private FavoriteController favoriteController;

    private static final String USER_ID = "1001";

    private static AuthUser currentUser() {
        return new AuthUser(USER_ID, "tester");
    }

    @Test
    @DisplayName("获取收藏列表成功")
    void testGetFavorites() {
        Favorite favorite = Favorite.reconstitute("1", "1001", "2001", null);
        PageResult<Favorite> pageResult = PageResult.of(List.of(favorite), 1L, 1, 10);

        FavoriteResponse favoriteResponse =
                FavoriteResponse.builder().id("1").productId("2001").build();
        PageResult<FavoriteResponse> responsePageResult = PageResult.of(List.of(favoriteResponse), 1L, 1, 10);

        when(favoriteService.queryFavorites(USER_ID, 1, 10)).thenReturn(pageResult);
        when(favoriteAssembler.toPageResult(pageResult, 1, 10)).thenReturn(responsePageResult);

        var result = favoriteController.getFavorites(currentUser(), 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data().total()).isEqualTo(1L);
        assertThat(result.data().records().get(0).getId()).isEqualTo("1");
        assertThat(result.data().records().get(0).getProductId()).isEqualTo("2001");
        verify(favoriteService).queryFavorites(USER_ID, 1, 10);
    }

    @Test
    @DisplayName("添加收藏成功")
    void testAddFavorite() {
        doNothing().when(favoriteService).addFavorite(eq(USER_ID), anyString());

        var result = favoriteController.addFavorite(currentUser(), "2001");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(favoriteService).addFavorite(USER_ID, "2001");
    }

    @Test
    @DisplayName("移除收藏成功")
    void testRemoveFavorite() {
        doNothing().when(favoriteService).removeFavorite(eq(USER_ID), anyString());

        var result = favoriteController.removeFavorite(currentUser(), "2001");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(favoriteService).removeFavorite(USER_ID, "2001");
    }

    @Test
    @DisplayName("批量移除收藏成功")
    void testRemoveManyFavorites() {
        BatchRemoveRequest request = new BatchRemoveRequest();
        request.setIds(List.of("1", "2", "3"));
        doNothing().when(favoriteService).removeManyFavorites(eq(USER_ID), any());

        var result = favoriteController.removeManyFavorites(currentUser(), request);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(favoriteService).removeManyFavorites(USER_ID, List.of("1", "2", "3"));
    }

    @Test
    @DisplayName("检查是否收藏 - 已收藏")
    void testCheckIsFavorited_True() {
        when(favoriteService.isFavorited(USER_ID, "2001")).thenReturn(true);

        var result = favoriteController.checkIsFavorited(currentUser(), "2001");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data()).isTrue();
    }

    @Test
    @DisplayName("检查是否收藏 - 未收藏")
    void testCheckIsFavorited_False() {
        when(favoriteService.isFavorited(USER_ID, "2001")).thenReturn(false);

        var result = favoriteController.checkIsFavorited(currentUser(), "2001");

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data()).isFalse();
    }

    @Test
    @DisplayName("获取收藏数量")
    void testGetFavoriteCount() {
        when(favoriteService.getFavoriteCount(USER_ID)).thenReturn(5L);

        var result = favoriteController.getFavoriteCount(currentUser());

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data()).isEqualTo(5L);
    }

    @Test
    @DisplayName("批量检查收藏状态")
    void testBatchCheckFavorited() {
        Map<String, Boolean> checkResult = Map.of("2001", true, "2002", false);
        when(favoriteService.batchCheckFavorited(USER_ID, List.of("2001", "2002"))).thenReturn(checkResult);

        BatchCheckRequest request = new BatchCheckRequest();
        request.setProductIds(List.of("2001", "2002"));
        var result = favoriteController.batchCheckFavorited(currentUser(), request);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.data()).hasSize(2);
        assertThat(result.data().get("2001")).isTrue();
        assertThat(result.data().get("2002")).isFalse();
    }
}
