package com.openSupports.demo.api.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateFolderCmd {
    @NotBlank(message = "主题名称不能为空")
    private String name;

    private Boolean isPrivate = false;
}
