package com.openSupports.demo.api.dto.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeStaffPasswordCmd {
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 200, message = "密码长度6-200字符")
    private String newPassword;
}
