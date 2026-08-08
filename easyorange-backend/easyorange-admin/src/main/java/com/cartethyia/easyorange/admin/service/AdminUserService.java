package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserQueryResult;
import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(CommonConstant.DATETIME_FORMAT);

    private final AdminUserQueryPort adminUserQueryPort;

    public PageResult<AdminUserResponse> listUsers(AdminUserQueryRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        UserQueryResult result = adminUserQueryPort.queryUsers(new UserQueryCondition(
                request.getKeyword(),
                request.getUserType(),
                request.getStatus(),
                parseStartTime(request.getStartTime()),
                parseEndTime(request.getEndTime()),
                pageNum,
                pageSize));

        List<AdminUserResponse> records =
                result.records().stream().map(this::toAdminUserResponse).toList();

        return PageResult.of(records, result.total(), pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserDetail(String id) {
        UserDetail user = adminUserQueryPort.getUserDetail(id);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        return toAdminUserResponse(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(String id, UpdateStatusRequest request) {
        adminUserQueryPort.updateUserStatus(id, request.getStatus());
    }

    private LocalDateTime parseStartTime(String startTime) {
        if (!StringUtils.hasText(startTime)) {
            return null;
        }
        try {
            return LocalDateTime.parse(startTime + " 00:00:00", DATE_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseEndTime(String endTime) {
        if (!StringUtils.hasText(endTime)) {
            return null;
        }
        try {
            return LocalDateTime.parse(endTime + " 23:59:59", DATE_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private AdminUserResponse toAdminUserResponse(UserDetail user) {
        return AdminUserResponse.builder()
                .userId(user.id())
                .username(user.username())
                .nickname(user.nickName())
                .avatar(user.avatar())
                .email(user.email())
                .phone(user.phone())
                .studentId(user.studentId())
                .realName(user.realName())
                .userType(user.userType())
                .userTypeDesc(user.userTypeDesc())
                .status(user.status())
                .statusDesc(user.statusDesc())
                .loginIp(user.loginIp())
                .loginDate(user.loginDate())
                .createTime(user.createTime())
                .updateTime(user.updateTime())
                .build();
    }
}
