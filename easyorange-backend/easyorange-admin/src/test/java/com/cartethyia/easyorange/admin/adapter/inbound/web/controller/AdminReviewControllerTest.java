package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.dto.response.AdminReviewResponse;
import com.cartethyia.easyorange.admin.service.AdminReviewService;
import com.cartethyia.easyorange.common.result.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminReviewService adminReviewService;

    @Test
    void listReviews_shouldReturnPaginatedReviews() throws Exception {
        var reviews = List.of(
            AdminReviewResponse.builder().reviewId(1L).productId(100L).productName("Product1")
                .userId(10L).username("user1").rating(5).content("好评！")
                .likes(3).status(1).createTime(LocalDateTime.of(2026, 5, 16, 10, 0)).build(),
            AdminReviewResponse.builder().reviewId(2L).productId(100L).productName("Product1")
                .userId(11L).username("user2").rating(4).content("不错")
                .likes(1).status(1).createTime(LocalDateTime.of(2026, 5, 16, 11, 0)).build()
        );
        var pageResult = PageResult.of(reviews, 2L, 1, 20);
        when(adminReviewService.listReviews(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/reviews"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.records.length()").value(2))
            .andExpect(jsonPath("$.data.records[0].reviewId").value(1))
            .andExpect(jsonPath("$.data.records[0].rating").value(5))
            .andExpect(jsonPath("$.data.records[0].content").value("好评！"))
            .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void listReviews_withRatingFilter_shouldFilterByRating() throws Exception {
        var reviews = List.of(
            AdminReviewResponse.builder().reviewId(1L).rating(5).content("好评").build()
        );
        var pageResult = PageResult.of(reviews, 1L, 1, 20);
        when(adminReviewService.listReviews(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/reviews?rating=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records[0].rating").value(5));
    }

    @Test
    void getReviewDetail_found_shouldReturnReview() throws Exception {
        var review = AdminReviewResponse.builder().reviewId(1L).productId(100L).productName("Product1")
            .userId(10L).username("user1").rating(5).content("好评！")
            .likes(3).status(1).createTime(LocalDateTime.of(2026, 5, 16, 10, 0)).build();
        when(adminReviewService.getReviewDetail(1L)).thenReturn(review);

        mockMvc.perform(get("/api/admin/reviews/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.reviewId").value(1))
            .andExpect(jsonPath("$.data.productName").value("Product1"))
            .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    void deleteReview_withReason_shouldSucceed() throws Exception {
        doNothing().when(adminReviewService).deleteReview(eq(1L), any());

        mockMvc.perform(delete("/api/admin/reviews/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"包含违规内容\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void deleteReview_withoutReason_shouldReturn400() throws Exception {
        mockMvc.perform(delete("/api/admin/reviews/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}