package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cartethyia.easyorange.common.entity.BaseDO;
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

    private String username;

    @JsonIgnore
    private String password;

    private UserType userType;

    private String email;

    private String phone;

    private String studentId;

    private String realName;

    private String nickName;

    private Sex sex;

    private UserStatus status;

    private String loginIp;

    private LocalDateTime loginDate;

    private LocalDateTime pwdUpdateDate;

    private String avatar;

    private String remark;

    @Version
    private Integer version;
}
