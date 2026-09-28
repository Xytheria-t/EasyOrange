package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.application.dto.UserView;
import com.cartethyia.easyorange.user.domain.aggregate.ContactUpdateSpec;
import com.cartethyia.easyorange.user.domain.aggregate.PersonalUpdateSpec;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.AvatarFilePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.ProfileUpdateService;
import com.cartethyia.easyorange.user.domain.valueobject.Avatar;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资料应用服务 — 个人信息与头像的读写编排。
 * <p>
 * 取舍：更新走「读改写」整聚合落库（不逐列 update），用户表窄、并发低，换取聚合规则单点；
 * 空字段视为「不改」而非「清空」，故先经 {@code hasAny} 判定无事可做直接拒绝。
 * 头像走「<b>先删旧文件再传新文件</b>」：旧头像地址一旦失效必须同步清理，否则会留下
 * 用户已放弃的孤儿文件；代价是传新失败时头像为空，DB 事务回滚也救不回已删的旧文件。
 * <p>
 * 边界：头像大小在 {@link Avatar#validate(byte[])} 内、落盘前拦截（无副作用）；
 * 落盘 / 落库任一步异常统一转为「头像上传失败」，不回抛存储层异常；
 * 邮箱 / 手机号唯一性由 {@code ProfileUpdateService} 校验，格式由值对象兜底。
 */
@Service
@RequiredArgsConstructor
public class ProfileAppService {

    private final UserRepository userRepository;
    private final AvatarFilePort avatarFilePort;
    private final ProfileUpdateService profileUpdateService;

    public record UpdateCommand(String nickname, String email, String phone, String gender, String realName) {}

    @Transactional(readOnly = true)
    public UserView getCurrentUser(String userId) {
        return UserView.from(findUserOrThrow(userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public UserView updateUserInfo(String userId, UpdateCommand cmd) {
        User currentUser = findUserOrThrow(userId);
        if (!hasAny(cmd)) throw BusinessException.of("没有需要更新的字段");

        profileUpdateService.validateUniqueContact(cmd.email(), cmd.phone(), currentUser);

        var updated = currentUser
                .updateContactInfo(new ContactUpdateSpec(cmd.email(), cmd.phone()), currentUser.getId())
                .updatePersonalInfo(
                        new PersonalUpdateSpec(
                                cmd.realName(),
                                cmd.nickname(),
                                cmd.gender() != null ? Sex.fromCode(cmd.gender()) : null),
                        currentUser.getId());

        userRepository.update(updated);
        return UserView.from(updated);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserView uploadAvatar(String userId, byte[] content, String contentType, String filename) {
        Avatar.validate(content);

        User currentUser = findUserOrThrow(userId);
        var currentAvatar = Optional.ofNullable(currentUser.getPersonalInfo())
                .map(PersonalInfo::avatar)
                .orElse(null);
        avatarFilePort.deleteIfExists(currentAvatar);

        try {
            var avatarUrl = avatarFilePort.upload(content, contentType, filename, currentUser.getId());
            Avatar avatar = Avatar.uploaded(avatarUrl, content, contentType);
            User updated = currentUser.changeAvatar(avatar, currentUser.getId());
            userRepository.update(updated);
            return UserView.from(updated);
        } catch (Exception e) {
            throw BusinessException.of("头像上传失败", e);
        }
    }

    /** 更新命令是否携带任何需要应用的字段（为空则无事可做，直接拒绝）。 */
    private boolean hasAny(UpdateCommand cmd) {
        return isPresent(cmd.nickname())
                || isPresent(cmd.email())
                || isPresent(cmd.phone())
                || cmd.gender() != null
                || isPresent(cmd.realName());
    }

    private User findUserOrThrow(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> BusinessException.of(UserResultCode.USER_NOT_FOUND));
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
