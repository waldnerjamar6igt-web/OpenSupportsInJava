package com.openSupports.demo.api.dto.staff;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StaffOverviewView {
    private Long id;
    private String email;
    private Integer level;
    private Long departmentId;
    private String departmentName;
    private int ticketAssignedCount;
    private String state;
    private LocalDateTime signUpTime;
}
