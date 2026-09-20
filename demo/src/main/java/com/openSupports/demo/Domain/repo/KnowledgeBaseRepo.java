package com.openSupports.demo.Domain.repo;

import com.openSupports.demo.Domain.Model.Article;
import com.openSupports.demo.Domain.Model.Folder;
import java.util.List;

public interface KnowledgeBaseRepo {
    Article getArticleById(Long articleId);
    List<Article> getByFolderId(Long folderId, boolean includePrivate);
    Article createAndSave(Article article);
    void update(Article article);
    void deleteById(Long articleId);
    List<Folder> findAllPublicFolders();
    List<Folder> findAllIncludingPrivate();
    Folder getFolderById(Long folderId);
    Folder createFolder(Folder folder);
    void updateFolder(Folder folder);
    void updateState(Long folderId, boolean isPrivate);
    void reorderFolders(List<Long> orderedIds);
    void removeFolder(Long folderId);
    void moveArticlesToNullFolder(Long folderId);
}
