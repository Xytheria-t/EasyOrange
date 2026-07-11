package com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;

@TableName("eo_category")
public class CategoryDO extends BaseDO {

    private String name;
    private String parentId;
    private Integer level;
    private String icon;
    private Integer sortOrder;
    private Integer status;

    public CategoryDO() {
    }

    public CategoryDO(String name, String parentId, Integer level, String icon, Integer sortOrder, Integer status) {
        this.name = name;
        this.parentId = parentId;
        this.level = level;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String parentId;
        private Integer level;
        private String icon;
        private Integer sortOrder;
        private Integer status;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder parentId(String parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder level(Integer level) {
            this.level = level;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder sortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder status(Integer status) {
            this.status = status;
            return this;
        }

        public CategoryDO build() {
            return new CategoryDO(name, parentId, level, icon, sortOrder, status);
        }
    }
}
