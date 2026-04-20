package com.cartethyia.easyorange.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 实体类基类，提供通用的审计字段和逻辑删除功能
 */
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public abstract class BaseDO {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间
     * <p>
     * 序列化格式由全局 Jackson 配置统一管理，DO 层不指定。
     * </p>
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * <p>
     * 序列化格式由全局 Jackson 配置统一管理，DO 层不指定。
     * </p>
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人 ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人 ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 删除标志：0-正常，2-已删除
     * <p>
     * 与 application.yml 中 mybatis-plus.global-config.db-config 配置保持一致。
     * 注解优先级高于全局配置，因此必须显式指定正确的值。
     * </p>
     */
    @TableLogic(value = "0", delval = "2")
    private Integer delFlag;

    /**
     * 乐观锁版本号
     * <p>
     * 用于并发控制，更新时自动递增。需在 MyBatis-Plus 配置中注册 OptimisticLockerInnerInterceptor。
     * </p>
     */
    @Version
    private Integer version;
}
