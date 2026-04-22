package com.cartethyia.easyorange.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;

import java.time.LocalDateTime;

@TableName("search_history")
public class SearchHistory extends BaseDO {

    private Long userId;
    private String keyword;
    private LocalDateTime searchTime;

    public SearchHistory() {
    }

    public SearchHistory(Long userId, String keyword, LocalDateTime searchTime) {
        this.userId = userId;
        this.keyword = keyword;
        this.searchTime = searchTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public LocalDateTime getSearchTime() {
        return searchTime;
    }

    public void setSearchTime(LocalDateTime searchTime) {
        this.searchTime = searchTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String keyword;
        private LocalDateTime searchTime;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder searchTime(LocalDateTime searchTime) {
            this.searchTime = searchTime;
            return this;
        }

        public SearchHistory build() {
            return new SearchHistory(userId, keyword, searchTime);
        }
    }
}