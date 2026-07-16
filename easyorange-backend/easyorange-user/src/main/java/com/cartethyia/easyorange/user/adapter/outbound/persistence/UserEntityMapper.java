package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.function.Function;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
@SuppressWarnings("unused")
public interface UserEntityMapper {

    @Mapping(target = "encodedPassword", source = "password")
    Credentials toCredentials(UserEntity entity);

    ContactInfo toContactInfo(UserEntity entity);

    LoginInfo toLoginInfo(UserEntity entity);

    AuditInfo toAuditInfo(UserEntity entity);

    PersonalInfo toPersonalInfo(UserEntity entity);

    User toDomain(UserEntity entity);

    default UserEntity from(User user) {
        if (user == null) {
            return null;
        }
        return UserEntity.builder()
            .id(user.getId())
            .username(user.getUsername())
            .password(user.getPassword())
            .userType(user.getUserType())
            .status(user.getStatus())
            .email(safeGet(user.getContactInfo(), ContactInfo::email))
            .phone(safeGet(user.getContactInfo(), ContactInfo::phone))
            .realName(safeGet(user.getPersonalInfo(), PersonalInfo::realName))
            .nickName(safeGet(user.getPersonalInfo(), PersonalInfo::nickName))
            .sex(safeGet(user.getPersonalInfo(), PersonalInfo::sex))
            .studentId(safeGet(user.getPersonalInfo(), PersonalInfo::studentId))
            .avatar(safeGet(user.getPersonalInfo(), PersonalInfo::avatar))
            .loginIp(safeGet(user.getLoginInfo(), LoginInfo::loginIp))
            .loginDate(safeGet(user.getLoginInfo(), LoginInfo::loginDate))
            .pwdUpdateDate(safeGet(user.getLoginInfo(), LoginInfo::pwdUpdateDate))
            .createTime(safeGet(user.getAuditInfo(), AuditInfo::createTime))
            .updateTime(safeGet(user.getAuditInfo(), AuditInfo::updateTime))
            .createBy(safeGet(user.getAuditInfo(), AuditInfo::createBy))
            .updateBy(safeGet(user.getAuditInfo(), AuditInfo::updateBy))
            .delFlag(safeGet(user.getAuditInfo(), AuditInfo::delFlag))
            .version(safeGet(user.getAuditInfo(), AuditInfo::version))
            .build();
    }

    private static <T, R> R safeGet(T source, Function<T, R> extractor) {
        return source == null ? null : extractor.apply(source);
    }
}
