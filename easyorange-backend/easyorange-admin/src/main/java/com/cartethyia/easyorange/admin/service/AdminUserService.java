package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(CommonConstant.DATETIME_FORMAT);

    private final UserMapper userMapper;

    public PageResult<AdminUserResponse> listUsers(AdminUserQueryRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        var wrapper = ChainWrappers.lambdaQueryChain(userMapper).eq(UserDO::getDelFlag, 0);

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(UserDO::getUsername, request.getKeyword())
                    .or()
                    .like(UserDO::getNickName, request.getKeyword())
                    .or()
                    .like(UserDO::getEmail, request.getKeyword())
                    .or()
                    .like(UserDO::getPhone, request.getKeyword()));
        }

        if (StringUtils.hasText(request.getUserType())) {
            wrapper.eq(UserDO::getUserType, UserType.fromCode(request.getUserType()));
        }

        if (StringUtils.hasText(request.getStatus())) {
            UserStatus status = UserStatus.values()[Integer.parseInt(request.getStatus())];
            wrapper.eq(UserDO::getStatus, status);
        }

        if (StringUtils.hasText(request.getStartTime())) {
            try {
                LocalDateTime startTime = LocalDateTime.parse(request.getStartTime() + " 00:00:00", DATE_FORMATTER);
                wrapper.ge(UserDO::getCreateTime, startTime);
            } catch (Exception ignored) {
            }
        }

        if (StringUtils.hasText(request.getEndTime())) {
            try {
                LocalDateTime endTime = LocalDateTime.parse(request.getEndTime() + " 23:59:59", DATE_FORMATTER);
                wrapper.le(UserDO::getCreateTime, endTime);
            } catch (Exception ignored) {
            }
        }

        wrapper.orderByDesc(UserDO::getCreateTime);

        Page<UserDO> page = wrapper.page(new Page<>(pageNum, pageSize));

        List<AdminUserResponse> records =
                page.getRecords().stream().map(this::toAdminUserResponse).collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserDetail(String id) {
        UserDO entity = userMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("用户不存在");
        }
        return toAdminUserResponse(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(String id, UpdateStatusRequest request) {
        UserDO entity = userMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("用户不存在");
        }

        UserStatus newStatus;
        try {
            newStatus = UserStatus.fromCode(request.getStatus());
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of("无效的用户状态");
        }
        entity.setStatus(newStatus);
        userMapper.updateById(entity);
    }

    private AdminUserResponse toAdminUserResponse(UserDO entity) {
        return AdminUserResponse.builder()
                .userId(entity.getId())
                .username(entity.getUsername())
                .nickname(entity.getNickName())
                .avatar(entity.getAvatar())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .studentId(entity.getStudentId())
                .realName(entity.getRealName())
                .userType(entity.getUserType() != null ? entity.getUserType().getCode() : null)
                .userTypeDesc(
                        entity.getUserType() != null ? entity.getUserType().getDescription() : null)
                .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
                .statusDesc(entity.getStatus() != null ? entity.getStatus().getDescription() : null)
                .loginIp(entity.getLoginIp())
                .loginDate(entity.getLoginDate())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
