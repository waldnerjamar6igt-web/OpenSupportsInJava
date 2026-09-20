package com.openSupports.demo.api.dto.staff;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeStaffLevelCmd {
    @NotNull(message = "级别不能为空")
    @Min(value = 1, message = "级别必须在1-3之间")
    @Max(value = 3, message = "级别必须在1-3之间")
    private Integer level;
}
