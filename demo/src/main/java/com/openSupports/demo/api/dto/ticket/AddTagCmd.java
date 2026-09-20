package com.openSupports.demo.api.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTagCmd {
    @NotBlank(message = "标签名称不能为空")
    private String tagName;
}
