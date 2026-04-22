package com.cartethyia.easyorange.user.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordCommand implements UserCommand {

    private Long userId;

    private String oldPassword;

    private String newPassword;
}