package com.cartethyia.easyorange.user.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserCommand {
    private String nickname;
    private String email;
    private String phone;
    private Integer gender;
    private String realName;
    private String studentId;
}
