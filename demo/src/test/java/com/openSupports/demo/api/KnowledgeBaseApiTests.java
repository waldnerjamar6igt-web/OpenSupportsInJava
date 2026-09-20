package com.openSupports.demo.api;

import com.openSupports.demo.ApiTestBase;
import com.openSupports.demo.api.dto.common.ApiError;
import com.openSupports.demo.api.dto.common.OperationResult;
import com.openSupports.demo.api.dto.knowledge.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** UC-16 / UC-16b / UC-17 知识库与文章 */
class KnowledgeBaseApiTests extends ApiTestBase {

    private String l2() {
        return cookie(L2_EMAILS[0]);
    }

    private Long createFolder(String name, boolean isPrivate) {
        ResponseEntity<OperationResult> resp = post("/api/v1/kb/folders",
                Map.of("name", name, "isPrivate", isPrivate), l2(), OperationResult.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return findFolderId(name);
    }

    private Long findFolderId(String name) {
        ResponseEntity<KnowledgeFolderView[]> resp = get("/api/v1/kb/folders", null, KnowledgeFolderView[].class);
        return java.util.Arrays.stream(resp.getBody())
                .filter(f -> name.equals(f.getName()))
                .map(KnowledgeFolderView::getId)
                .findFirst()
                .orElseGet(() -> {
                    // 私有主题不会出现在公开列表中，退而用管理接口原始数据
                    return null;
                });
    }

    private Long createArticle(String title, String content, Long folderId) {
        ResponseEntity<OperationResult> resp = post("/api/v1/kb/articles",
                Map.of("title", title, "content", content, "folderId", folderId), l2(), OperationResult.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 通过公开文章详情反查 id：先列出该 folder 的文章
        ResponseEntity<KnowledgeFolderView[]> folders = get("/api/v1/kb/folders", null, KnowledgeFolderView[].class);
        for (KnowledgeFolderView f : folders.getBody()) {
            if (f.getArticles() != null) {
                for (ArticleListItemView a : f.getArticles()) {
                    if (title.equals(a.getTitle())) {
                        return a.getId();
                    }
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // UC-16 公开浏览
    // ------------------------------------------------------------------
    @Test
    void uc16_publicCanBrowsePublicFoldersAndArticles() {
        String folderName = "KB public " + uniqueSuffix();
        Long folderId = createFolder(folderName, false);
        String articleTitle = "KB article " + uniqueSuffix();
        Long articleId = createArticle(articleTitle, "body content", folderId);
        assertThat(articleId).isNotNull();

        ResponseEntity<KnowledgeFolderView[]> folders = get("/api/v1/kb/folders", null, KnowledgeFolderView[].class);
        assertThat(folders.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(folders.getBody()).extracting(KnowledgeFolderView::getName).contains(folderName);

        ArticleDetailView article = get("/api/v1/kb/articles/" + articleId, null, ArticleDetailView.class).getBody();
        assertThat(article.getContent()).isEqualTo("body content");
        assertThat(article.getFolderName()).isEqualTo(folderName);
    }

    @Test
    void uc16_privateFolderHiddenFromPublic() {
        String folderName = "KB private " + uniqueSuffix();
        Long folderId = createFolder(folderName, false);
        String articleTitle = "KB private article " + uniqueSuffix();
        Long articleId = createArticle(articleTitle, "secret", folderId);
        assertThat(articleId).isNotNull();

        // 将主题设为私有
        assertThat(put("/api/v1/kb/folders/" + folderId + "/state", Map.of("isPrivate", true),
                l2(), OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<KnowledgeFolderView[]> folders = get("/api/v1/kb/folders", null, KnowledgeFolderView[].class);
        assertThat(folders.getBody()).extracting(KnowledgeFolderView::getName).doesNotContain(folderName);

        // 私有主题下文章对公众不可见
        assertThat(get("/api/v1/kb/articles/" + articleId, null, ApiError.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------
    // UC-16b 主题管理
    // ------------------------------------------------------------------
    @Test
    void uc16b_folderLifecycleRenameStateOrderDelete() {
        String folderName = "KB mgmt " + uniqueSuffix();
        Long folderId = createFolder(folderName, false);

        // 重命名
        assertThat(put("/api/v1/kb/folders/" + folderId, Map.of("newName", folderName + "-renamed"),
                l2(), OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        // 设为私有，再公开
        assertThat(put("/api/v1/kb/folders/" + folderId + "/state", Map.of("isPrivate", true),
                l2(), OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(put("/api/v1/kb/folders/" + folderId + "/state", Map.of("isPrivate", false),
                l2(), OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        // 排序
        Long otherId = createFolder("KB order " + uniqueSuffix(), false);
        assertThat(put("/api/v1/kb/folders/order", Map.of("folderOrder", List.of(otherId, folderId)),
                l2(), OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        // 删除
        assertThat(delete("/api/v1/kb/folders/" + folderId, l2(), OperationResult.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void uc16b_managementRequiresL2() {
        // 未登录
        assertThat(post("/api/v1/kb/folders", Map.of("name", "x"), null, ApiError.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        // L1 不够权限
        assertThat(post("/api/v1/kb/folders", Map.of("name", "x " + uniqueSuffix()),
                cookie(L1_EMAILS[0]), ApiError.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        // 普通用户不够权限
        assertThat(post("/api/v1/kb/folders", Map.of("name", "x " + uniqueSuffix()),
                cookie(USER_EMAILS[0]), ApiError.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------
    // UC-17 文章管理
    // ------------------------------------------------------------------
    @Test
    void uc17_articleLifecycle() {
        Long folderId = createFolder("KB article mgmt " + uniqueSuffix(), false);
        String title = "KB art " + uniqueSuffix();
        Long articleId = createArticle(title, "v1", folderId);
        assertThat(articleId).isNotNull();

        // 更新
        assertThat(put("/api/v1/kb/articles/" + articleId,
                Map.of("title", title + "-v2", "content", "v2", "folderId", folderId),
                l2(), OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        ArticleDetailView updated = get("/api/v1/kb/articles/" + articleId, null, ArticleDetailView.class).getBody();
        assertThat(updated.getTitle()).isEqualTo(title + "-v2");
        assertThat(updated.getContent()).isEqualTo("v2");

        // 删除
        assertThat(delete("/api/v1/kb/articles/" + articleId, l2(), OperationResult.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get("/api/v1/kb/articles/" + articleId, null, ApiError.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
