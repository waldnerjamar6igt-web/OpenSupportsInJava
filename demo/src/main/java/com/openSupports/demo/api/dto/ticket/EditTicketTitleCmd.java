package com.openSupports.demo.api.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditTicketTitleCmd {
    @NotBlank(message = "标题不能为空")
    private String newTitle;
}
