package com.openSupports.demo.application.service;

import com.openSupports.demo.Domain.Model.Account;
import com.openSupports.demo.Domain.Model.Article;
import com.openSupports.demo.Domain.Model.Folder;
import com.openSupports.demo.Domain.repo.AccountRepo;
import com.openSupports.demo.Domain.repo.KnowledgeBaseRepo;
import com.openSupports.demo.api.dto.knowledge.*;
import com.openSupports.demo.infra.exception.PermissionDeniedException;
import com.openSupports.demo.infra.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepo knowledgeBaseRepo;
    private final AccountRepo accountRepo;

    // ------------------------------------------------------------------
    // UC-16 知识库浏览（公开）
    // ------------------------------------------------------------------
    public List<KnowledgeFolderView> getPublicFolders() {
        return knowledgeBaseRepo.findAllPublicFolders().stream()
                .map(this::toFolderView)
                .toList();
    }

    public ArticleDetailView getArticleDetail(Long articleId) {
        var article = knowledgeBaseRepo.getArticleById(articleId);
        if (article == null) {
            throw new ResourceNotFoundException("Article", articleId);
        }
        var folder = article.getFolderId() != null ? knowledgeBaseRepo.getFolderById(article.getFolderId()) : null;
        if (folder != null && Boolean.TRUE.equals(folder.getIsPrivate())) {
            throw new PermissionDeniedException("无权查看该文章");
        }
        return toArticleDetailView(article);
    }

    // ------------------------------------------------------------------
    // UC-16b 知识库主题管理（L2+）
    // ------------------------------------------------------------------
    @Transactional
    public void createFolder(Long actorId, CreateFolderCmd cmd) {
        requireManager(actorId);
        Folder folder = new Folder();
        folder.setName(cmd.getName());
        folder.setIsPrivate(Boolean.TRUE.equals(cmd.getIsPrivate()));
        knowledgeBaseRepo.createFolder(folder);
    }

    @Transactional
    public void updateFolder(Long actorId, Long folderId, UpdateFolderCmd cmd) {
        requireManager(actorId);
        var folder = knowledgeBaseRepo.getFolderById(folderId);
        if (folder == null) {
            throw new ResourceNotFoundException("Folder", folderId);
        }
        folder.rename(cmd.getNewName());
        knowledgeBaseRepo.updateFolder(folder);
    }

    @Transactional
    public void deleteFolder(Long actorId, Long folderId) {
        requireManager(actorId);
        var folder = knowledgeBaseRepo.getFolderById(folderId);
        if (folder == null) {
            throw new ResourceNotFoundException("Folder", folderId);
        }
        // 主题下文章进入“未分类”（folder_id = NULL）
        knowledgeBaseRepo.moveArticlesToNullFolder(folderId);
        knowledgeBaseRepo.removeFolder(folderId);
    }

    @Transactional
    public void updateFolderOrder(Long actorId, UpdateFolderOrderCmd cmd) {
        requireManager(actorId);
        knowledgeBaseRepo.reorderFolders(cmd.getFolderOrder());
    }

    @Transactional
    public void changeFolderState(Long actorId, Long folderId, ChangeFolderStateCmd cmd) {
        requireManager(actorId);
        if (knowledgeBaseRepo.getFolderById(folderId) == null) {
            throw new ResourceNotFoundException("Folder", folderId);
        }
        knowledgeBaseRepo.updateState(folderId, Boolean.TRUE.equals(cmd.getIsPrivate()));
    }

    // ------------------------------------------------------------------
    // UC-17 文章管理（L2+）
    // ------------------------------------------------------------------
    @Transactional
    public void createArticle(Long actorId, CreateArticleCmd cmd) {
        requireManager(actorId);
        if (cmd.getFolderId() != null && knowledgeBaseRepo.getFolderById(cmd.getFolderId()) == null) {
            throw new ResourceNotFoundException("Folder", cmd.getFolderId());
        }
        Article article = new Article();
        article.setTitle(cmd.getTitle());
        article.setContent(cmd.getContent());
        article.setFolderId(cmd.getFolderId());
        knowledgeBaseRepo.createAndSave(article);
    }

    @Transactional
    public void updateArticle(Long actorId, Long articleId, UpdateArticleCmd cmd) {
        requireManager(actorId);
        var article = knowledgeBaseRepo.getArticleById(articleId);
        if (article == null) {
            throw new ResourceNotFoundException("Article", articleId);
        }
        article.updateContent(cmd.getTitle(), cmd.getContent(), cmd.getFolderId());
        knowledgeBaseRepo.update(article);
    }

    @Transactional
    public void deleteArticle(Long actorId, Long articleId) {
        requireManager(actorId);
        if (knowledgeBaseRepo.getArticleById(articleId) == null) {
            throw new ResourceNotFoundException("Article", articleId);
        }
        knowledgeBaseRepo.deleteById(articleId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private void requireManager(Long actorId) {
        Account account = actorId != null ? accountRepo.findById(actorId) : null;
        if (account == null || !account.isStaff() || account.getLevel() == null || account.getLevel() < 2) {
            throw new PermissionDeniedException("需要 L2 及以上员工权限");
        }
    }

    private KnowledgeFolderView toFolderView(Folder folder) {
        KnowledgeFolderView view = new KnowledgeFolderView();
        view.setId(folder.getId());
        view.setName(folder.getName());
        view.setIsPrivate(folder.getIsPrivate());
        view.setSortOrder(folder.getSortOrder());
        if (folder.getArticles() != null) {
            view.setArticles(folder.getArticles().stream().map(a -> {
                ArticleListItemView av = new ArticleListItemView();
                av.setId(a.getId());
                av.setTitle(a.getTitle());
                av.setFolderName(a.getFolderName());
                av.setUpdatedAt(a.getUpdatedAt());
                return av;
            }).toList());
        }
        return view;
    }

    private ArticleDetailView toArticleDetailView(Article article) {
        ArticleDetailView view = new ArticleDetailView();
        view.setId(article.getId());
        view.setTitle(article.getTitle());
        view.setContent(article.getContent());
        view.setFolderId(article.getFolderId());
        view.setFolderName(article.getFolderName());
        view.setCreatedAt(article.getCreatedAt());
        view.setUpdatedAt(article.getUpdatedAt());
        return view;
    }
}
