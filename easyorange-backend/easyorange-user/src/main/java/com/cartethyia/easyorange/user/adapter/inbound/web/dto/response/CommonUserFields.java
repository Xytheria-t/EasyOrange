package com.cartethyia.easyorange.user.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;

public interface CommonUserFields {
    void setNickname(String v);
    void setEmail(String v);
    void setPhone(String v);
    void setRealName(String v);
    void setAvatar(String v);
    void setStudentId(String v);
    void setStatus(String v);
    void setCreateTime(LocalDateTime v);
    void setUpdateTime(LocalDateTime v);
}
