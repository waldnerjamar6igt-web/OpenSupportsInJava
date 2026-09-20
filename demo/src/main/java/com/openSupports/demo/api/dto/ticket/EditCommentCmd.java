package com.openSupports.demo.api.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditCommentCmd {
    @NotBlank(message = "内容不能为空")
    private String content;
}
