package com.openSupports.demo.api.dto.staff;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeStaffStateCmd {
    @NotNull(message = "状态不能为空")
    private Boolean enabled;
}
