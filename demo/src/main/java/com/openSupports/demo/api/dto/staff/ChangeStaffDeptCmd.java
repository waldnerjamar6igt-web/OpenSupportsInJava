package com.openSupports.demo.api.dto.staff;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeStaffDeptCmd {
    @NotNull(message = "目标部门ID不能为空")
    private Long targetDepartmentId;
}
