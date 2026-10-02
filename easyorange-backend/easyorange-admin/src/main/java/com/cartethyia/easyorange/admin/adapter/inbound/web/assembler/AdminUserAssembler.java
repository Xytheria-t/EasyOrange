package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.admin.domain.model.DayRange;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryResult;
import com.cartethyia.easyorange.common.result.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 管理端用户视图组装 — 查询串解析 + 出参命名都在这一层，服务层只见端口记录。
 *
 * <p>时间过滤走 {@link DayRange}：当天边界的口径由它定一次，订单 / 商品 / 用户三个列表共用，
 * 同一句查询在三个接口里含义必须一致。
 */
@Slf4j
@Component
public class AdminUserAssembler {

    public UserQueryCondition toCondition(AdminUserQueryRequest request) {
        DayRange range = DayRange.of(request.startTime(), request.endTime());
        return new UserQueryCondition(
                request.keyword(),
                request.userType(),
                request.status(),
                range.start(),
                range.end(),
                request.pageNum(),
                request.pageSize());
    }

    /** 新密码只在这次响应里出现一次，DB 存哈希、无法再取回，故必须给运营一个可转达的提示。 */
    public ResetPasswordResponse toResetPasswordResponse(String newPassword) {
        return ResetPasswordResponse.builder()
                .newPassword(newPassword)
                .message("密码已重置，请将新密码安全地传递给用户")
                .build();
    }

    public PageResult<AdminUserResponse> toPageResponse(UserQueryResult result) {
        return PageResult.of(
                result.records().stream().map(this::toResponse).toList(),
                result.total(),
                result.pageNum(),
                result.pageSize());
    }

    public AdminUserResponse toResponse(UserDetail user) {
        return AdminUserResponse.builder()
                .userId(user.id())
                .username(user.username())
                .nickname(user.nickName())
                .avatar(user.avatar())
                .email(user.email())
                .phone(user.phone())
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
