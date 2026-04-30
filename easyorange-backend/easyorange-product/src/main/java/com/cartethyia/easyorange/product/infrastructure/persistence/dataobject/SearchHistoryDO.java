package com.cartethyia.easyorange.product.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;

import java.time.LocalDateTime;

@TableName("search_history")
public class SearchHistoryDO extends BaseDO {

    private Long userId;
    private String keyword;
    private LocalDateTime searchTime;

    public SearchHistoryDO() {
    }

    public SearchHistoryDO(Long userId, String keyword, LocalDateTime searchTime) {
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

        public SearchHistoryDO build() {
            return new SearchHistoryDO(userId, keyword, searchTime);
        }
    }
}
