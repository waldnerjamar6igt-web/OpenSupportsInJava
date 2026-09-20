package com.openSupports.demo.api.dto.knowledge;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeFolderView {
    private Long id;
    private String name;
    private Boolean isPrivate;
    private int sortOrder;
    private List<ArticleListItemView> articles;
}
