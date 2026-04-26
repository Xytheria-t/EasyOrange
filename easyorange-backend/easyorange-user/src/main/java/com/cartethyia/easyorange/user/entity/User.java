package com.cartethyia.easyorange.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.cartethyia.easyorange.user.enums.LoginType;
import com.cartethyia.easyorange.user.enums.Sex;
import com.cartethyia.easyorange.user.enums.UserStatus;
import com.cartethyia.easyorange.user.enums.UserType;
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
 */
@TableName("sys_user")
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
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

    @TableField("user_type")
    private UserType userType;

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
    private LoginType loginType;

    private Sex sex;

    private UserStatus status;

    @TableField("login_ip")
    private String loginIp;

    @TableField("login_date")
    private LocalDateTime loginDate;

    @TableField("pwd_update_date")
    private LocalDateTime pwdUpdateDate;

    @TableField("avatar")
    private String avatar;

    private String remark;
}
