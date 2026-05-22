package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.ImmutablePersonalInfo;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.function.Function;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL,
    imports = {
        Credentials.class,
        ContactInfo.class,
        ImmutablePersonalInfo.class,
        LoginInfo.class,
        AuditInfo.class
    }
)
public interface UserEntityMapper {

    @Mapping(target = "credentials", expression = "java(new Credentials(entity.getUsername(), entity.getPassword()))")
    @Mapping(target = "contactInfo", expression = "java(new ContactInfo(entity.getEmail(), entity.getPhone()))")
    @Mapping(target = "personalInfo", expression = "java(ImmutablePersonalInfo.builder().realName(entity.getRealName()).nickName(entity.getNickName()).sex(entity.getSex()).studentId(entity.getStudentId()).avatar(entity.getAvatar()).build())")
    @Mapping(target = "loginInfo", expression = "java(new LoginInfo(entity.getLoginIp(), entity.getLoginDate(), entity.getPwdUpdateDate()))")
    @Mapping(target = "auditInfo", expression = "java(new AuditInfo(entity.getCreateTime(), entity.getUpdateTime(), entity.getCreateBy(), entity.getUpdateBy(), entity.getDelFlag(), entity.getVersion()))")
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
            .version(user.getAuditInfo() != null ? user.getAuditInfo().version() : 0)
            .build();
    }

    private static <T, R> R safeGet(T source, Function<T, R> extractor) {
        return source == null ? null : extractor.apply(source);
    }
}
