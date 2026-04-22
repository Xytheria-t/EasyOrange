package com.cartethyia.easyorange.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseDO {

    /**
     * 主键 ID（覆盖 BaseDO 的 ASSIGN_ID，使用数据库自增）
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @JsonIgnore
    private String password;

    /**
     * 用户类型：01-普通用户，02-管理员
     */
    @TableField("user_type")
    private String userType;

    private String email;

    @TableField("phonenumber")
    private String phone;

    @TableField("student_id")
    private String studentId;

    @TableField("real_name")
    private String realName;

    @TableField("nick_name")
    private String nickName;

    @TableField("login_type")
    private String loginType;

    private String sex;

    private String status;

    @TableField("login_ip")
    private String loginIp;

    @TableField("login_date")
    private LocalDateTime loginDate;

    @TableField("pwd_update_date")
    private LocalDateTime pwdUpdateDate;

    private String remark;
}
