package com.openSupports.demo.api.dto.knowledge;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeFolderStateCmd {
    @NotNull(message = "状态不能为空")
    private Boolean isPrivate;
}
