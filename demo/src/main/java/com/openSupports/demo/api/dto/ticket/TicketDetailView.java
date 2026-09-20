package com.openSupports.demo.api.dto.ticket;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TicketDetailView {
    private Long id;
    private String ticketCode;
    private String title;
    private String content;
    private String status;
    private String priority;
    private String departmentName;
    private Long departmentId;
    private String assigneeName;
    private Long assigneeId;
    private Long authorId;
    private String authorEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActivityAt;
    private boolean editedTitle;
    private List<String> tags;
    private List<CommentView> comments;
}
