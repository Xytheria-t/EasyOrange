package com.cartethyia.easyorange.user.domain.port;

import java.util.Collection;
import java.util.List;

/**
 * 用户只读查询端口 — 供其他模块经 ACL 读取用户公开信息。
 * 跨模块禁止直接依赖 {@code UserRepository} / 聚合根，统一走本端口。
 */
public interface UserQueryPort {

    /** 批量查询用户公开信息（按传入 ID 顺序，缺失的 ID 不返回）。 */
    List<UserInfo> findAllByIds(Collection<String> ids);

    /** 用户总数（未删除）。 */
    long count();

    /** 用户公开信息投影（不含凭据/联系方式等敏感字段）。 */
    record UserInfo(String id, String username, String nickName, String avatar) {}
}
