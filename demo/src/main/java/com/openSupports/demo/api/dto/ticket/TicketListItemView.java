package com.openSupports.demo.api.dto.ticket;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketListItemView {
    private Long id;
    private String ticketCode;
    private String title;
    private String status;
    private String priority;
    private String departmentName;
    private String assigneeName;
    private Long authorId;
    private String authorEmail;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private int unreadCount;
    private boolean editedTitle;
}
