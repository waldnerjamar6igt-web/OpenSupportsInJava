package com.openSupports.demo.api.dto.staff;

import com.openSupports.demo.api.dto.ticket.TicketListItemView;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DetailedUserView {
    private Long id;
    private String email;
    private LocalDateTime signUpTime;
    private String state;
    private List<TicketListItemView> tickets;
}
