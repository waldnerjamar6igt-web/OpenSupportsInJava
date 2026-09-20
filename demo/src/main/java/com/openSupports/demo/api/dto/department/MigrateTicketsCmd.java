package com.openSupports.demo.api.dto.department;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MigrateTicketsCmd {
    @NotNull(message = "目标部门ID不能为空")
    private Long targetDepartmentId;
}
