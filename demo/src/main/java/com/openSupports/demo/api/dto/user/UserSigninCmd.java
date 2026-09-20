package com.openSupports.demo.api.dto.user;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserSigninCmd {
    @Email(message = "邮箱格式不正确")
    private String email;
    private String password;
    private String rememberToken;
    private Long userId;
    private Boolean rememberMe = false;
}
