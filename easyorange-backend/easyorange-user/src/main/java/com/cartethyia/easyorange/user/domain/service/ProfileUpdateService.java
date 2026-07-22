package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * 用户资料更新领域服务 — 聚合用户资料变更所需的业务规则校验和编排。
 * 将应用层 ProfileAppService 中的唯一性校验逻辑下沉到领域层，保持 domain 封装。
 */
@RequiredArgsConstructor
public class ProfileUpdateService {

    private final UserRepository userRepository;

    /**
     * 校验联系方式（邮箱、手机号）和学号的唯一性。
     *
     * @param email       待校验邮箱
     * @param phone       待校验手机号
     * @param studentId   待校验学号
     * @param currentUser 当前用户聚合根
     */
    public void validateUniqueContact(String email, String phone, String studentId, User currentUser) {
        var contact = currentUser.getContactInfo();
        if (isPresent(email) && !email.equals(contact != null ? contact.email() : null)
            && userRepository.findByEmail(email).isPresent())
            throw BusinessException.of(UserResultCode.EMAIL_EXISTS);
        if (isPresent(phone) && !phone.equals(contact != null ? contact.phone() : null)
            && userRepository.findByPhone(phone).isPresent())
            throw BusinessException.of(UserResultCode.PHONE_EXISTS);

        var personal = currentUser.getPersonalInfo();
        if (isPresent(studentId) && !studentId.equals(personal != null ? personal.studentId() : null)
            && userRepository.findByStudentId(studentId).isPresent())
            throw BusinessException.of(UserResultCode.STUDENT_ID_EXISTS);
    }

    /**
     * 判断是否有任何字段需要更新。
     */
    public static boolean hasAny(String nickname, String email, String phone,
                                  String gender, String realName, String studentId) {
        return isPresent(nickname) || isPresent(email) || isPresent(phone)
            || gender != null || isPresent(realName) || isPresent(studentId);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
