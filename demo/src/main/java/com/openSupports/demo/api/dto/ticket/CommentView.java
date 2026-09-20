package com.openSupports.demo.api.dto.ticket;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentView {
    private Long id;
    private String content;
    private Long authorId;
    private String authorType;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean privateNote;
    private boolean edited;
}
