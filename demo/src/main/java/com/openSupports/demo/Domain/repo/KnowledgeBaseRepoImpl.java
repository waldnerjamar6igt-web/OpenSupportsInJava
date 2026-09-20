package com.openSupports.demo.Domain.repo;

import com.openSupports.demo.Domain.Model.Article;
import com.openSupports.demo.Domain.Model.Folder;
import com.openSupports.demo.infra.mapper.ArticleMapper;
import com.openSupports.demo.infra.mapper.FolderMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class KnowledgeBaseRepoImpl implements KnowledgeBaseRepo {

    private final FolderMapper folderMapper;
    private final ArticleMapper articleMapper;

    public KnowledgeBaseRepoImpl(FolderMapper folderMapper, ArticleMapper articleMapper) {
        this.folderMapper = folderMapper;
        this.articleMapper = articleMapper;
    }

    @Override
    public Article getArticleById(Long articleId) {
        return articleMapper.getById(articleId);
    }

    @Override
    public List<Article> getByFolderId(Long folderId, boolean includePrivate) {
        return articleMapper.getByFolderId(folderId, includePrivate);
    }

    @Override
    public Article createAndSave(Article article) {
        articleMapper.createAndSave(article);
        return article;
    }

    @Override
    public void update(Article article) {
        articleMapper.update(article);
    }

    @Override
    public void deleteById(Long articleId) {
        articleMapper.deleteById(articleId);
    }

    @Override
    public List<Folder> findAllPublicFolders() {
        List<Folder> folders = folderMapper.findAllPublic();
        folders.forEach(f -> f.setArticles(articleMapper.getByFolderId(f.getId(), false)));
        return folders;
    }

    @Override
    public List<Folder> findAllIncludingPrivate() {
        List<Folder> folders = folderMapper.findAllIncludingPrivate();
        folders.forEach(f -> f.setArticles(articleMapper.getByFolderId(f.getId(), true)));
        return folders;
    }

    @Override
    public Folder getFolderById(Long folderId) {
        return folderMapper.findById(folderId);
    }

    @Override
    public Folder createFolder(Folder folder) {
        folderMapper.insert(folder);
        return folder;
    }

    @Override
    public void updateFolder(Folder folder) {
        folderMapper.updateName(folder.getId(), folder.getName());
    }

    @Override
    public void updateState(Long folderId, boolean isPrivate) {
        folderMapper.updateState(folderId, isPrivate);
    }

    @Override
    public void reorderFolders(List<Long> orderedIds) {
        if (orderedIds != null && !orderedIds.isEmpty()) {
            folderMapper.reorder(orderedIds);
        }
    }

    @Override
    public void removeFolder(Long folderId) {
        folderMapper.remove(folderId);
    }

    @Override
    public void moveArticlesToNullFolder(Long folderId) {
        folderMapper.moveArticlesToNullFolder(folderId);
    }
}
