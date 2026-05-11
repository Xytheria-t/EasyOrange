package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.admin.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminUserVO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PageResult<AdminUserVO> listUsers(AdminUserQueryRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::getDelFlag, 0);

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                .like(UserEntity::getUsername, request.getKeyword())
                .or()
                .like(UserEntity::getNickName, request.getKeyword())
                .or()
                .like(UserEntity::getEmail, request.getKeyword())
                .or()
                .like(UserEntity::getPhone, request.getKeyword())
            );
        }

        if (StringUtils.hasText(request.getUserType())) {
            UserType userType = UserType.fromCode(request.getUserType());
            if (userType != null) {
                wrapper.eq(UserEntity::getUserType, userType);
            }
        }

        if (StringUtils.hasText(request.getStatus())) {
            UserStatus status = UserStatus.values()[Integer.parseInt(request.getStatus())];
            wrapper.eq(UserEntity::getStatus, status);
        }

        if (StringUtils.hasText(request.getStartTime())) {
            try {
                LocalDateTime startTime = LocalDateTime.parse(request.getStartTime() + " 00:00:00", DATE_FORMATTER);
                wrapper.ge(UserEntity::getCreateTime, startTime);
            } catch (Exception ignored) {
            }
        }

        if (StringUtils.hasText(request.getEndTime())) {
            try {
                LocalDateTime endTime = LocalDateTime.parse(request.getEndTime() + " 23:59:59", DATE_FORMATTER);
                wrapper.le(UserEntity::getCreateTime, endTime);
            } catch (Exception ignored) {
            }
        }

        wrapper.orderByDesc(UserEntity::getCreateTime);

        Page<UserEntity> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<AdminUserVO> records = page.getRecords().stream()
            .map(this::toAdminUserVO)
            .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public AdminUserVO getUserDetail(Long id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("用户不存在");
        }
        return toAdminUserVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long id, UpdateStatusRequest request) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("用户不存在");
        }

        UserStatus newStatus = UserStatus.values()[request.getStatus()];
        entity.setStatus(newStatus);
        userMapper.updateById(entity);
    }

    private AdminUserVO toAdminUserVO(UserEntity entity) {
        return AdminUserVO.builder()
            .userId(entity.getId())
            .username(entity.getUsername())
            .nickname(entity.getNickName())
            .avatar(entity.getAvatar())
            .email(entity.getEmail())
            .phone(entity.getPhone())
            .studentId(entity.getStudentId())
            .realName(entity.getRealName())
            .userType(entity.getUserType() != null ? entity.getUserType().getCode() : null)
            .userTypeDesc(entity.getUserType() != null ? entity.getUserType().getDescription() : null)
            .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
            .statusDesc(entity.getStatus() != null ? entity.getStatus().getDescription() : null)
            .loginIp(entity.getLoginIp())
            .loginDate(entity.getLoginDate())
            .createTime(entity.getCreateTime())
            .updateTime(entity.getUpdateTime())
            .build();
    }
}
