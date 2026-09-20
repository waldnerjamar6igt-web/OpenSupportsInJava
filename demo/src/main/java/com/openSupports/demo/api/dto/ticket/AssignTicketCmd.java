package com.openSupports.demo.api.dto.ticket;

import lombok.Data;

@Data
public class AssignTicketCmd {
    /** 被分配员工ID；L1/L2 只能分配给自己，L3 可分配给任何人（含跨部门）。为空表示分配给自己。 */
    private Long staffId;
}
