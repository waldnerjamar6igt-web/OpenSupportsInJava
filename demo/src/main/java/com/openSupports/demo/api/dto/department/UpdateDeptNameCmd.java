package com.openSupports.demo.api.dto.department;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateDeptNameCmd {
    @NotBlank(message = "部门名称不能为空")
    private String newName;
}
