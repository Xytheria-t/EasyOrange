package com.cartethyia.easyorange.user.application.assembler;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserProfileVO;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserVO;
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
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    UserProfileVO toProfileVo(User user, Set<String> roles, Set<String> permissions, Long loginTime);

    default LoginResponse toLoginResponse(User user, String accessToken, String refreshToken) {
        return LoginResponse.builder()
            .token(accessToken)
            .refreshToken(refreshToken)
            .user(toVo(user))
            .build();
    }

    @AfterMapping
    default void applyMaskingAndConversions(User user, @MappingTarget UserVO vo) {
        if (user == null) return;
        String email = user.getProfile() != null ? user.getProfile().email() : null;
        String phone = user.getProfile() != null ? user.getProfile().phone() : null;
        String realName = user.getProfile() != null ? user.getProfile().realName() : null;
        String nickName = user.getProfile() != null ? user.getProfile().nickName() : null;
        String avatar = user.getProfile() != null ? user.getProfile().avatar() : null;

        vo.setNickname(nickName);
        vo.setEmail(MaskUtils.maskEmail(email));
        vo.setPhone(MaskUtils.maskPhone(phone));
        vo.setRealName(MaskUtils.maskName(realName));
        vo.setAvatar(avatar);
        vo.setStatus(user.getStatus() != null
            ? Integer.parseInt(user.getStatus().getCode()) : 0);
        vo.setCreateTime(user.getAuditInfo() != null ? user.getAuditInfo().createTime() : null);
        vo.setUpdateTime(user.getAuditInfo() != null ? user.getAuditInfo().updateTime() : null);
    }

    @AfterMapping
    default void applyMaskingAndConversions(User user, @MappingTarget UserProfileVO vo) {
        if (user == null) return;
        String email = user.getProfile() != null ? user.getProfile().email() : null;
        String phone = user.getProfile() != null ? user.getProfile().phone() : null;
        String realName = user.getProfile() != null ? user.getProfile().realName() : null;
        String nickName = user.getProfile() != null ? user.getProfile().nickName() : null;
        String avatar = user.getProfile() != null ? user.getProfile().avatar() : null;
        var sex = user.getProfile() != null ? user.getProfile().sex() : null;

        vo.setNickname(nickName);
        vo.setEmail(MaskUtils.maskEmail(email));
        vo.setPhone(MaskUtils.maskPhone(phone));
        vo.setRealName(MaskUtils.maskName(realName));
        vo.setAvatar(avatar);
        vo.setStatus(user.getStatus() != null
            ? Integer.parseInt(user.getStatus().getCode()) : 0);
        vo.setStatusDesc(user.getStatus() != null
            ? user.getStatus().getDescription() : null);
        vo.setGender(sex != null
            ? Integer.parseInt(sex.getCode()) : null);
        vo.setUserType(user.getUserType() != null
            ? user.getUserType().getDescription() : null);
    }
}
