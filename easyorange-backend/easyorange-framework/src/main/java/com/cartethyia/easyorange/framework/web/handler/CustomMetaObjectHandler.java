package com.cartethyia.easyorange.framework.web.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

@Component
public class CustomMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        var now = LocalDateTime.now();
        var userId = SecurityContextUtil.getCurrentUserId().orElse(null);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", String.class, userId);
        this.strictInsertFill(metaObject, "updateBy", String.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(
                metaObject,
                "updateBy",
                String.class,
                SecurityContextUtil.getCurrentUserId().orElse(null));
    }
}
