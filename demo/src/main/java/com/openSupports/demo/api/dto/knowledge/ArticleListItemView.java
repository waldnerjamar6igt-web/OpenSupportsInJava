package com.openSupports.demo.api.dto.knowledge;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleListItemView {
    private Long id;
    private String title;
    private String folderName;
    private LocalDateTime updatedAt;
}
