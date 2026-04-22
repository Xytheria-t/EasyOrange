package com.cartethyia.easyorange.user.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordCommand implements UserCommand {

    private String phone;

    private String newPassword;
}