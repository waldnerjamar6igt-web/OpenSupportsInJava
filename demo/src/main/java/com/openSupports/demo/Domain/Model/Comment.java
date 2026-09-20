package com.openSupports.demo.Domain.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private Long id;
    private Long ticketId;
    private Long authorId;
    private String authorType;
    private String content;
    private Boolean isPrivate;
    private Boolean isEdited;
    private Long editedBy;
    private LocalDateTime editedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String authorName;
}
