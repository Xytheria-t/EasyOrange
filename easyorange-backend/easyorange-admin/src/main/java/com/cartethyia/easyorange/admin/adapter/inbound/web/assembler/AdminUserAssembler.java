package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryResult;
import com.cartethyia.easyorange.common.result.PageResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 管理端用户视图组装 — 查询串解析 + 出参命名都在这一层，服务层只见端口记录。
 * <p>
 * <b>取舍</b>：日期解析失败按「不过滤」处理并记 warn，而不是 400 ——
 * 管理端多传一个手滑的日期就把整页数据查不出来，比漏掉该过滤条件更难排查。
 */
@Slf4j
@Component
public class AdminUserAssembler {

    public UserQueryCondition toCondition(AdminUserQueryRequest request) {
        return new UserQueryCondition(
                request.keyword(),
                request.userType(),
                request.status(),
                parseStart(request.startTime()),
                parseEnd(request.endTime()),
                request.pageNum(),
                request.pageSize());
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

    private LocalDateTime parseStart(String startTime) {
        if (!StringUtils.hasText(startTime)) {
            return null;
        }
        try {
            return LocalDate.parse(startTime).atStartOfDay();
        } catch (DateTimeParseException e) {
            log.warn("action=user_query_start_time_unparsable, value={}, expected=yyyy-MM-dd", startTime);
            return null;
        }
    }

    private LocalDateTime parseEnd(String endTime) {
        if (!StringUtils.hasText(endTime)) {
            return null;
        }
        try {
            // 结束日期含当天：给时间戳而不是当天零点，否则「查 5 月」会漏掉 5 月 31 日
            return LocalDate.parse(endTime).atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            log.warn("action=user_query_end_time_unparsable, value={}, expected=yyyy-MM-dd", endTime);
            return null;
        }
    }
}
