package com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;

import java.time.LocalDateTime;

@TableName("hot_keyword")
public class HotKeywordDO extends BaseDO {

    private String keyword;
    private Integer searchCount;
    private LocalDateTime lastSearchTime;

    public HotKeywordDO() {
    }

    public HotKeywordDO(String keyword, Integer searchCount, LocalDateTime lastSearchTime) {
        this.keyword = keyword;
        this.searchCount = searchCount;
        this.lastSearchTime = lastSearchTime;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(Integer searchCount) {
        this.searchCount = searchCount;
    }

    public LocalDateTime getLastSearchTime() {
        return lastSearchTime;
    }

    public void setLastSearchTime(LocalDateTime lastSearchTime) {
        this.lastSearchTime = lastSearchTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String keyword;
        private Integer searchCount;
        private LocalDateTime lastSearchTime;

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder searchCount(Integer searchCount) {
            this.searchCount = searchCount;
            return this;
        }

        public Builder lastSearchTime(LocalDateTime lastSearchTime) {
            this.lastSearchTime = lastSearchTime;
            return this;
        }

        public HotKeywordDO build() {
            return new HotKeywordDO(keyword, searchCount, lastSearchTime);
        }
    }
}
