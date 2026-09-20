package com.openSupports.demo.api.dto.staff;

import com.openSupports.demo.api.dto.ticket.TicketListItemView;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DetailedStaffView {
    private Long id;
    private String email;
    private Integer level;
    private Long departmentId;
    private String departmentName;
    private int ticketAssignedCount;
    private String state;
    private LocalDateTime signUpTime;
    private List<TicketListItemView> ticketsAssigned;
}
