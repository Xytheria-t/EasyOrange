package com.cartethyia.easyorange.product.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;

import java.time.LocalDateTime;

@TableName("eo_search_history")
public class SearchHistoryDO extends BaseDO {

    private String userId;
    private String keyword;
    private LocalDateTime searchTime;

    public SearchHistoryDO() {
    }

    public SearchHistoryDO(String userId, String keyword, LocalDateTime searchTime) {
        this.userId = userId;
        this.keyword = keyword;
        this.searchTime = searchTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
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
        private String userId;
        private String keyword;
        private LocalDateTime searchTime;

        public Builder userId(String userId) {
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

        public SearchHistoryDO build() {
            return new SearchHistoryDO(userId, keyword, searchTime);
        }
    }
}
