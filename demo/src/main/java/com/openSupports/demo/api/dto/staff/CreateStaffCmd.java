package com.openSupports.demo.api.dto.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStaffCmd {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 200, message = "密码长度6-200字符")
    private String password;

    @NotNull(message = "级别不能为空")
    @Min(value = 1, message = "级别必须在1-3之间")
    @Max(value = 3, message = "级别必须在1-3之间")
    private Integer level;

    @NotNull(message = "部门不能为空")
    private Long departmentId;
}
