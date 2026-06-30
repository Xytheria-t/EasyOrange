package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@TableName("eo_user")
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseDO {

    @TableId(value = "user_id", type = IdType.INPUT)
    private String id;

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
