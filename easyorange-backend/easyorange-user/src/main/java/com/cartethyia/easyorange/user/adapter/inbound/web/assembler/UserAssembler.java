package com.cartethyia.easyorange.user.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.CommonUserFields;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResult;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserProfileResponse;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserResponse;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import java.time.LocalDateTime;
import java.util.Set;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface UserAssembler {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "nickname", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "realName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "studentId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    UserResponse toResponse(User user);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "nickname", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "realName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "statusDesc", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "userType", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "studentId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    UserProfileResponse toProfileResponse(User user, Set<String> roles, Set<String> permissions, Long loginTime);

    default LoginResult toLoginResult(User user, String accessToken) {
        return new LoginResult(accessToken, toResponse(user));
    }

    @AfterMapping
    default void afterToResponse(User user, @MappingTarget UserResponse r) {
        if (user == null) return;
        CommonData.from(user).applyTo(r);
        r.setUserType(user.getUserType());
    }

    @AfterMapping
    default void afterToProfileResponse(User user, @MappingTarget UserProfileResponse r) {
        if (user == null) return;
        CommonData.from(user).applyTo(r);
        UserStatus userStatus = user.getStatus();
        PersonalInfo personalInfo = user.getPersonalInfo();
        r.setStatusDesc(userStatus != null ? userStatus.getDescription() : null);
        r.setGender(genderCode(personalInfo));
        r.setUserType(user.getUserType());
    }

    private static String genderCode(PersonalInfo info) {
        if (info == null) return null;
        var sex = info.sex();
        return sex != null ? sex.getCode() : null;
    }

    record CommonData(
            String status,
            LocalDateTime createTime,
            LocalDateTime updateTime,
            String nickname,
            String email,
            String phone,
            String realName,
            String avatar,
            String studentId) {

        static CommonData from(User user) {
            ContactInfo ci = user.getContactInfo();
            PersonalInfo pi = user.getPersonalInfo();
            AuditInfo ai = user.getAuditInfo();
            var st = user.getStatus();
            var sc = st != null ? st.getCode() : null;
            return new CommonData(
                    sc,
                    ai != null ? ai.createTime() : null,
                    ai != null ? ai.updateTime() : null,
                    pi != null ? pi.nickName() : null,
                    MaskUtils.maskEmail(ci != null ? ci.email() : null),
                    MaskUtils.maskPhone(ci != null ? ci.phone() : null),
                    MaskUtils.maskName(pi != null ? pi.realName() : null),
                    pi != null ? pi.avatar() : null,
                    pi != null ? pi.studentId() : null);
        }

        void applyTo(CommonUserFields r) {
            r.setStatus(status);
            r.setCreateTime(createTime);
            r.setUpdateTime(updateTime);
            r.setNickname(nickname);
            r.setEmail(email);
            r.setPhone(phone);
            r.setRealName(realName);
            r.setAvatar(avatar);
            r.setStudentId(studentId);
        }
    }
}
