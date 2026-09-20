package com.openSupports.demo.Domain.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Article {
    private Long id;
    private String title;
    private String content;
    private Long folderId;
    private String folderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updateContent(String title, String content, Long folderId) {
        this.title = title;
        this.content = content;
        this.folderId = folderId;
        this.updatedAt = LocalDateTime.now();
    }
}
