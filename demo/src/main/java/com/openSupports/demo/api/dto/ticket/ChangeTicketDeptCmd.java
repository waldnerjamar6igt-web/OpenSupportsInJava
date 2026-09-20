package com.openSupports.demo.api.dto.ticket;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeTicketDeptCmd {
    @NotNull(message = "目标部门ID不能为空")
    private Long targetDepartmentId;
}
