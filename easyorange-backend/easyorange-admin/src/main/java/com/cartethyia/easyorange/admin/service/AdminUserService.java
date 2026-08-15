package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryResult;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserPort adminUserPort;

    public PageResult<AdminUserResponse> listUsers(AdminUserQueryRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        UserQueryResult result = adminUserPort.queryUsers(new UserQueryCondition(
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
        UserDetail user = adminUserPort.getUserDetail(id);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        return toAdminUserResponse(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(String id, UpdateStatusRequest request) {
        adminUserPort.updateUserStatus(id, request.getStatus());
    }

    private LocalDateTime parseStartTime(String startTime) {
        if (!StringUtils.hasText(startTime)) {
            return null;
        }
        try {
            return LocalDate.parse(startTime).atStartOfDay();
        } catch (DateTimeParseException e) {
            log.warn("无法解析时间: {}, 格式应为 yyyy-MM-dd", startTime);
            return null;
        }
    }

    private LocalDateTime parseEndTime(String endTime) {
        if (!StringUtils.hasText(endTime)) {
            return null;
        }
        try {
            return LocalDate.parse(endTime).atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            log.warn("无法解析时间: {}, 格式应为 yyyy-MM-dd", endTime);
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
