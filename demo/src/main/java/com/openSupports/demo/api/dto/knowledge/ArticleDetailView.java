package com.openSupports.demo.api.dto.knowledge;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleDetailView {
    private Long id;
    private String title;
    private String content;
    private Long folderId;
    private String folderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
