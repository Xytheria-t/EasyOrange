package com.cartethyia.easyorange.user.application.assembler;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.user.application.command.LoginResult;
import com.cartethyia.easyorange.user.application.dto.UserProfileVO;
import com.cartethyia.easyorange.user.application.dto.UserVO;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueMappingStrategy;

import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
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
    UserVO toVo(User user);

    @Mapping(target = "id", source = "user.id")
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
    UserProfileVO toProfileVo(User user, Set<String> roles, Set<String> permissions, Long loginTime);

    default LoginResult toLoginResult(User user, String accessToken, String refreshToken) {
        return new LoginResult(accessToken, refreshToken, toVo(user));
    }

    @AfterMapping
    default void applyMaskingAndConversions(User user, @MappingTarget UserVO vo) {
        if (user == null) return;
        var contactInfo = user.getContactInfo();
        var personalInfo = user.getPersonalInfo();

        String email = contactInfo != null ? contactInfo.email() : null;
        String phone = contactInfo != null ? contactInfo.phone() : null;
        String realName = personalInfo != null ? personalInfo.realName() : null;
        String nickName = personalInfo != null ? personalInfo.nickName() : null;
        String avatar = personalInfo != null ? personalInfo.avatar() : null;
        String studentId = personalInfo != null ? personalInfo.studentId() : null;

        vo.setNickname(nickName);
        vo.setEmail(MaskUtils.maskEmail(email));
        vo.setPhone(MaskUtils.maskPhone(phone));
        vo.setRealName(MaskUtils.maskName(realName));
        vo.setAvatar(avatar);
        vo.setStudentId(studentId);
        vo.setStatus(user.getStatus() != null
            ? Integer.parseInt(user.getStatus().getCode()) : 0);
        vo.setUserType(user.getUserType());
        vo.setCreateTime(user.getAuditInfo() != null ? user.getAuditInfo().createTime() : null);
        vo.setUpdateTime(user.getAuditInfo() != null ? user.getAuditInfo().updateTime() : null);
    }

    @AfterMapping
    default void applyMaskingAndConversions(User user, @MappingTarget UserProfileVO vo) {
        if (user == null) return;
        var contactInfo = user.getContactInfo();
        var personalInfo = user.getPersonalInfo();

        String email = contactInfo != null ? contactInfo.email() : null;
        String phone = contactInfo != null ? contactInfo.phone() : null;
        String realName = personalInfo != null ? personalInfo.realName() : null;
        String nickName = personalInfo != null ? personalInfo.nickName() : null;
        String avatar = personalInfo != null ? personalInfo.avatar() : null;
        String studentId = personalInfo != null ? personalInfo.studentId() : null;
        var sex = personalInfo != null ? personalInfo.sex() : null;

        vo.setNickname(nickName);
        vo.setEmail(MaskUtils.maskEmail(email));
        vo.setPhone(MaskUtils.maskPhone(phone));
        vo.setRealName(MaskUtils.maskName(realName));
        vo.setAvatar(avatar);
        vo.setStudentId(studentId);
        vo.setStatus(user.getStatus() != null
            ? Integer.parseInt(user.getStatus().getCode()) : 0);
        vo.setStatusDesc(user.getStatus() != null
            ? user.getStatus().getDescription() : null);
        vo.setGender(sex != null
            ? Integer.parseInt(sex.getCode()) : null);
        vo.setUserType(user.getUserType() != null
            ? user.getUserType().getCode() : null);
    }
}
