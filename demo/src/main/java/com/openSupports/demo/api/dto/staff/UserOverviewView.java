package com.openSupports.demo.api.dto.staff;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserOverviewView {
    private Long id;
    private String email;
    private LocalDateTime signUpTime;
    private int ticketCount;
    private String state;
}
