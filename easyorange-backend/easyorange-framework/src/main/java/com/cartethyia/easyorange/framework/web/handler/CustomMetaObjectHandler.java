package com.cartethyia.easyorange.framework.web.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class CustomMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String userId = getCurrentUserId();

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", String.class, userId);
        this.strictInsertFill(metaObject, "updateBy", String.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", String.class, getCurrentUserId());
    }

    private String getCurrentUserId() {
        return SecurityContextUtil.getCurrentUserId().orElse(null);
    }
}
