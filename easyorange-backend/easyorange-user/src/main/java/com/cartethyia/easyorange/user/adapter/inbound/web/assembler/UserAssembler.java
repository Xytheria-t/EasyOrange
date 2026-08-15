package com.cartethyia.easyorange.user.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.CommonUserFields;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResult;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserProfileResponse;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserResponse;
import com.cartethyia.easyorange.user.application.dto.UserView;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * 用户视图 → 响应 DTO 转换（含脱敏、枚举转码）。
 * 手写普通类：MapStruct 版本全部字段 ignore + {@code @AfterMapping} 手工回填，
 * 注解处理器只是空转，改为显式映射更直白。
 */
@Component
public class UserAssembler {

    public UserResponse toResponse(UserView view) {
        var r = new UserResponse();
        r.setUserId(view.id());
        r.setUsername(view.username());
        CommonData.from(view).applyTo(r);
        r.setUserType(view.userType());
        return r;
    }

    public UserProfileResponse toProfileResponse(UserView view) {
        var r = new UserProfileResponse();
        r.setUserId(view.id());
        r.setUsername(view.username());
        CommonData.from(view).applyTo(r);
        UserStatus userStatus = view.status();
        PersonalInfo personalInfo = view.personalInfo();
        r.setStatusDesc(userStatus != null ? userStatus.getDescription() : null);
        r.setGender(genderCode(personalInfo));
        r.setUserType(view.userType());
        return r;
    }

    public LoginResult toLoginResult(UserView view, String accessToken) {
        return new LoginResult(accessToken, toResponse(view));
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

        static CommonData from(UserView view) {
            ContactInfo ci = view.contactInfo();
            PersonalInfo pi = view.personalInfo();
            AuditInfo ai = view.auditInfo();
            var st = view.status();
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
