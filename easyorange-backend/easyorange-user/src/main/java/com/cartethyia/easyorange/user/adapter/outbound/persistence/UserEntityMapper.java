package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import java.util.function.Function;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
@SuppressWarnings("unused")
public interface UserEntityMapper {

    @Mapping(target = "encodedPassword", source = "password")
    Credentials toCredentials(UserDO entity);

    ContactInfo toContactInfo(UserDO entity);

    LoginInfo toLoginInfo(UserDO entity);

    AuditInfo toAuditInfo(UserDO entity);

    PersonalInfo toPersonalInfo(UserDO entity);

    @Mapping(target = "credentials", expression = "java(toCredentials(entity))")
    @Mapping(target = "contactInfo", expression = "java(toContactInfo(entity))")
    @Mapping(target = "personalInfo", expression = "java(toPersonalInfo(entity))")
    @Mapping(target = "loginInfo", expression = "java(toLoginInfo(entity))")
    @Mapping(target = "auditInfo", expression = "java(toAuditInfo(entity))")
    User toDomain(UserDO entity);

    default UserDO from(User user) {
        if (user == null) {
            return null;
        }
        return UserDO.builder()
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
