package com.cartethyia.easyorange.user.converter;

import com.cartethyia.easyorange.user.dto.bo.*;
import com.cartethyia.easyorange.user.dto.request.*;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户模块对象转换器
 * 职责：处理 DTO -> BO 的转换
 * <p>
 * 说明：
 * - BO -> Entity 的转换由 BO 自身负责（封装业务规则）
 * - Entity -> VO 的转换在此定义
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    // ========== Request -> BO ==========

    RegisterBo toBo(RegisterRequest request);

    UpdateUserBo toBo(UpdateUserRequest request);

    ChangePasswordBo toBo(ChangePasswordRequest request);

    ForgotPasswordBo toBo(ForgotPasswordRequest request);

    default UploadAvatarBo toBo(MultipartFile avatar) {
        return new UploadAvatarBo(avatar);
    }

    // ========== Entity -> VO ==========

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "gender", expression = "java(entity.getSex() != null ? entity.getSex().ordinal() : null)")
    @Mapping(target = "userType", expression = "java(entity.getUserType() != null ? entity.getUserType().getDescription() : null)")
    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? entity.getStatus().getDescription() : null)")
    UserVO toVo(User entity);
}
