package com.openSupports.demo.Domain.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库主题 / Knowledge base folder.
 *
 * <p>主题:文章 = 1:n，文章:主题 = 1:1（UC-16）。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Folder {
    private Long id;
    private String name;
    private Boolean isPrivate = Boolean.FALSE;
    private int sortOrder;
    private LocalDateTime createdAt;
    private List<Article> articles = new ArrayList<>();

    public void rename(String newName) {
        this.name = newName;
    }
}
