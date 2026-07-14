package com.cartethyia.easyorange.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 数据对象基类，所有数据库实体 DO 继承此类。
 * <p>
 * 字段说明：
 * <ul>
 *   <li><b>id</b> — UUID v7（{@link IdType#INPUT}），由 {@code IdGenerator} 在应用层生成，支持分布式零协调。</li>
 *   <li><b>createTime / updateTime</b> — 由 MyBatis-Plus {@code MetaObjectHandler} 自动填充。</li>
 *   <li><b>createBy / updateBy</b> — 从 {@code SecurityContextHolder} 自动填充当前用户 ID。</li>
 *   <li><b>delFlag</b> — 逻辑删除，0=正常，1=已删除。使用 {@code @TableLogic} 自动追加 {@code WHERE del_flag = 0}。
 *      <br>为什么用 0/1 而不是 0/2：0/1 是社区事实标准，无歧义，不创造未定义的"预留"状态。</li>
 *   <li><b>version（乐观锁）</b> — 不在 BaseDO 中统一声明，而是按需添加到有并发写冲突风险的 DO 上
 *      （如 ProductDO、OrderDO、PaymentPO 等有状态机的实体）。append-only 表（审计日志、消息等）不扛此字段。</li>
 * </ul>
 */
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public abstract class BaseDO {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
