package com.cartethyia.easyorange.user.application.handler;

import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.application.query.GetUserQuery;
import com.cartethyia.easyorange.user.domain.aggregate.UserAggregate;
import com.cartethyia.easyorange.user.domain.repository.UserReadRepository;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetUserHandler {

    private final UserReadRepository userReadRepository;

    @Transactional(readOnly = true)
    public UserVO handle(GetUserQuery query) {
        Long userId = query.getUserId() != null ? query.getUserId() : SecurityContextUtil.getCurrentUserIdOrThrow();

        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return toUserVO(user);
    }

    private UserVO toUserVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .studentId(user.getStudentId())
                .realName(user.getRealName())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}