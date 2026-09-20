package com.openSupports.demo.api.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddCommentCmd {
    @NotBlank(message = "评论不能为空")
    private String content;

    /** 员工回复时可选择私密备注，对非员工隐藏。 */
    private Boolean privateNote = false;
}
