package com.openSupports.demo.api.controller;

import com.openSupports.demo.api.dto.common.OperationResult;
import com.openSupports.demo.api.dto.knowledge.*;
import com.openSupports.demo.application.service.KnowledgeBaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    // 公开浏览端点
    @GetMapping("/folders")
    public List<KnowledgeFolderView> getFolders() {
        return knowledgeBaseService.getPublicFolders();
    }

    @GetMapping("/articles/{id}")
    public ArticleDetailView getArticleDetail(@PathVariable Long id) {
        return knowledgeBaseService.getArticleDetail(id);
    }

    // 管理端点（L2+）
    @PostMapping("/folders")
    public OperationResult createFolder(@Valid @RequestBody CreateFolderCmd cmd, HttpServletRequest request) {
        knowledgeBaseService.createFolder((Long) request.getAttribute("currentUserId"), cmd);
        return new OperationResult(true, "主题已创建");
    }

    @PutMapping("/folders/{id}")
    public OperationResult updateFolder(@PathVariable Long id, @Valid @RequestBody UpdateFolderCmd cmd,
                                        HttpServletRequest request) {
        knowledgeBaseService.updateFolder((Long) request.getAttribute("currentUserId"), id, cmd);
        return new OperationResult(true, "主题已更新");
    }

    @DeleteMapping("/folders/{id}")
    public OperationResult deleteFolder(@PathVariable Long id, HttpServletRequest request) {
        knowledgeBaseService.deleteFolder((Long) request.getAttribute("currentUserId"), id);
        return new OperationResult(true, "主题已删除");
    }

    @PutMapping("/folders/order")
    public OperationResult updateFolderOrder(@Valid @RequestBody UpdateFolderOrderCmd cmd, HttpServletRequest request) {
        knowledgeBaseService.updateFolderOrder((Long) request.getAttribute("currentUserId"), cmd);
        return new OperationResult(true, "排序已更新");
    }

    @PutMapping("/folders/{id}/state")
    public OperationResult changeFolderState(@PathVariable Long id, @Valid @RequestBody ChangeFolderStateCmd cmd,
                                             HttpServletRequest request) {
        knowledgeBaseService.changeFolderState((Long) request.getAttribute("currentUserId"), id, cmd);
        return new OperationResult(true, "状态已更新");
    }

    @PostMapping("/articles")
    public OperationResult createArticle(@Valid @RequestBody CreateArticleCmd cmd, HttpServletRequest request) {
        knowledgeBaseService.createArticle((Long) request.getAttribute("currentUserId"), cmd);
        return new OperationResult(true, "文章已创建");
    }

    @PutMapping("/articles/{id}")
    public OperationResult updateArticle(@PathVariable Long id, @Valid @RequestBody UpdateArticleCmd cmd,
                                         HttpServletRequest request) {
        knowledgeBaseService.updateArticle((Long) request.getAttribute("currentUserId"), id, cmd);
        return new OperationResult(true, "文章已更新");
    }

    @DeleteMapping("/articles/{id}")
    public OperationResult deleteArticle(@PathVariable Long id, HttpServletRequest request) {
        knowledgeBaseService.deleteArticle((Long) request.getAttribute("currentUserId"), id);
        return new OperationResult(true, "文章已删除");
    }
}
