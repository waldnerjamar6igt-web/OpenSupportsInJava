package com.openSupports.demo.api;

import com.openSupports.demo.ApiTestBase;
import com.openSupports.demo.api.dto.common.ApiError;
import com.openSupports.demo.api.dto.common.OperationResult;
import com.openSupports.demo.api.dto.common.PageResult;
import com.openSupports.demo.api.dto.ticket.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** UC-04 ~ UC-11 工单域 */
class TicketApiTests extends ApiTestBase {

    private static final ParameterizedTypeReference<PageResult<TicketListItemView>> TICKET_PAGE =
            new ParameterizedTypeReference<>() {
            };

    private String user1() {
        return cookie(USER_EMAILS[0]);
    }

    private String user2() {
        return cookie(USER_EMAILS[1]);
    }

    private String l1() {
        return cookie(L1_EMAILS[0]);
    }

    private String l2() {
        return cookie(L2_EMAILS[0]);
    }

    private String l3() {
        return cookie(L3_EMAILS[0]);
    }

    // ------------------------------------------------------------------
    // UC-04 创建工单
    // ------------------------------------------------------------------
    @Test
    void uc04_createTicket_success() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC04 ticket " + uniqueSuffix());
        assertThat(ticket.getId()).isNotNull();
        assertThat(ticket.getTicketCode()).isNotBlank();
        assertThat(ticket.getStatus()).isEqualTo("OPEN");
        assertThat(ticket.getAuthorEmail()).isEqualTo(USER_EMAILS[0]);
        assertThat(ticket.getDepartmentId()).isEqualTo(deptAId);
    }

    @Test
    void uc04_createTicket_defaultDepartmentRejected() {
        Long defaultDeptId = departmentMapper.getDefault().getId();
        CreateTicketCmd cmd = new CreateTicketCmd();
        cmd.setTitle("default dept ticket");
        cmd.setContent("content");
        cmd.setDepartmentId(defaultDeptId);
        ResponseEntity<ApiError> resp = post("/api/v1/tickets", cmd, user1(), ApiError.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCode()).isEqualTo("INVALID_DEPARTMENT");
    }

    @Test
    void uc04_createTicket_unauthenticatedRejected() {
        CreateTicketCmd cmd = new CreateTicketCmd();
        cmd.setTitle("x");
        cmd.setContent("y");
        cmd.setDepartmentId(deptAId);
        ResponseEntity<ApiError> resp = post("/api/v1/tickets", cmd, null, ApiError.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------
    // UC-05 工单粗略视图
    // ------------------------------------------------------------------
    @Test
    void uc05_mySentTickets_containsOwnTickets() {
        String title = "UC05 sent " + uniqueSuffix();
        createTicket(user1(), deptAId, title);
        ResponseEntity<PageResult<TicketListItemView>> resp = getType("/api/v1/tickets/me-sent?page=1&size=100", user1(), TICKET_PAGE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getContent()).extracting(TicketListItemView::getTitle).contains(title);
    }

    @Test
    void uc05_newTicketsForDepartment() {
        String title = "UC05 new " + uniqueSuffix();
        createTicket(user1(), deptAId, title);
        ResponseEntity<PageResult<TicketListItemView>> resp = getType("/api/v1/tickets/new?page=1&size=100", l1(), TICKET_PAGE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getContent()).extracting(TicketListItemView::getTitle).contains(title);
    }

    @Test
    void uc05_allTicketsForDepartment_and_titleSearch() {
        String title = "UC05 all " + uniqueSuffix();
        createTicket(user1(), deptAId, title);

        ResponseEntity<PageResult<TicketListItemView>> all = getType("/api/v1/tickets/all?page=1&size=100", l1(), TICKET_PAGE);
        assertThat(all.getBody().getContent()).extracting(TicketListItemView::getTitle).contains(title);

        ResponseEntity<PageResult<TicketListItemView>> search =
                getType("/api/v1/tickets/search?q=" + title, l1(), TICKET_PAGE);
        assertThat(search.getBody().getContent()).extracting(TicketListItemView::getTitle).contains(title);
    }

    @Test
    void uc05_meAssignedTickets() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC05 assigned " + uniqueSuffix());
        Long l1Id = userId(L1_EMAILS[0]);
        put("/api/v1/tickets/" + ticket.getId() + "/assign", Map.of("staffId", l1Id), l1(), String.class);

        ResponseEntity<PageResult<TicketListItemView>> resp = getType("/api/v1/tickets/me-assigned?page=1&size=100", l1(), TICKET_PAGE);
        assertThat(resp.getBody().getContent()).extracting(TicketListItemView::getId).contains(ticket.getId());
    }

    // ------------------------------------------------------------------
    // UC-06 工单具体视图
    // ------------------------------------------------------------------
    @Test
    void uc06_ownerCanView_staffInDeptCanView_adminCanView_othersDenied() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC06 " + uniqueSuffix());
        String path = "/api/v1/tickets/" + ticket.getId();

        assertThat(get(path, user1(), TicketDetailView.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(path, l1(), TicketDetailView.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(path, cookie(L3_EMAILS[1]), TicketDetailView.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(path, user2(), ApiError.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------
    // UC-07 回复工单
    // ------------------------------------------------------------------
    @Test
    void uc07_userAndStaffCanComment_privateHiddenFromUser() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC07 " + uniqueSuffix());
        String path = "/api/v1/tickets/" + ticket.getId();

        CommentView userComment = post(path + "/comments", Map.of("content", "user says hi", "privateNote", false),
                user1(), CommentView.class).getBody();
        assertThat(userComment.getAuthorType()).isEqualTo("USER");

        CommentView staffComment = post(path + "/comments", Map.of("content", "staff internal", "privateNote", true),
                l1(), CommentView.class).getBody();
        assertThat(staffComment.getAuthorType()).isEqualTo("STAFF");

        TicketDetailView asUser = get(path, user1(), TicketDetailView.class).getBody();
        assertThat(asUser.getComments()).extracting(CommentView::getContent).contains("user says hi").doesNotContain("staff internal");

        TicketDetailView asStaff = get(path, l1(), TicketDetailView.class).getBody();
        assertThat(asStaff.getComments()).extracting(CommentView::getContent).contains("staff internal");
    }

    @Test
    void uc07_commentOnClosedTicket_rejected() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC07 closed " + uniqueSuffix());
        String path = "/api/v1/tickets/" + ticket.getId();
        put(path + "/close", null, user1(), OperationResult.class);

        ResponseEntity<ApiError> resp = post(path + "/comments", Map.of("content", "too late"), user1(), ApiError.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCode()).isEqualTo("TICKET_CLOSED");
    }

    // ------------------------------------------------------------------
    // UC-08 编辑工单
    // ------------------------------------------------------------------
    @Test
    void uc08_authorEditTitle_setsEditedFlag() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC08 " + uniqueSuffix());
        String path = "/api/v1/tickets/" + ticket.getId();

        ResponseEntity<OperationResult> resp = put(path + "/title", Map.of("newTitle", "edited title"), user1(), OperationResult.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        TicketDetailView reloaded = get(path, user1(), TicketDetailView.class).getBody();
        assertThat(reloaded.getTitle()).isEqualTo("edited title");
        assertThat(reloaded.isEditedTitle()).isTrue();
    }

    @Test
    void uc08_onlyLatestOwnCommentEditable() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC08 comment " + uniqueSuffix());
        String path = "/api/v1/tickets/" + ticket.getId();

        CommentView first = post(path + "/comments", Map.of("content", "first"), user1(), CommentView.class).getBody();
        CommentView second = post(path + "/comments", Map.of("content", "second"), user1(), CommentView.class).getBody();

        ResponseEntity<ApiError> notLatest = put("/api/v1/tickets/comments/" + first.getId(),
                Map.of("content", "edited first"), user1(), ApiError.class);
        assertThat(notLatest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(notLatest.getBody().getCode()).isEqualTo("NOT_LATEST_COMMENT");

        ResponseEntity<OperationResult> ok = put("/api/v1/tickets/comments/" + second.getId(),
                Map.of("content", "edited second"), user1(), OperationResult.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void uc08_nonAuthorCannotEditComment() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC08 other " + uniqueSuffix());
        String path = "/api/v1/tickets/" + ticket.getId();
        CommentView comment = post(path + "/comments", Map.of("content", "mine"), user1(), CommentView.class).getBody();

        ResponseEntity<ApiError> resp = put("/api/v1/tickets/comments/" + comment.getId(),
                Map.of("content", "hacked"), user2(), ApiError.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------
    // UC-09 标签
    // ------------------------------------------------------------------
    @Test
    void uc09_addDuplicateRemoveTag() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC09 " + uniqueSuffix());
        String path = "/api/v1/tickets/" + ticket.getId();
        String tag = "tag-" + uniqueSuffix();

        assertThat(post(path + "/tags", Map.of("tagName", tag), l1(), OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiError> dup = post(path + "/tags", Map.of("tagName", tag), l1(), ApiError.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(dup.getBody().getCode()).isEqualTo("TAG_EXISTS");

        TicketDetailView reloaded = get(path, l1(), TicketDetailView.class).getBody();
        assertThat(reloaded.getTags()).contains(tag);

        // 找到 tagId 后移除
        Long tagId = reloaded.getTags().isEmpty() ? null : findTagId(tag);
        assertThat(delete(path + "/tags/" + tagId, l1(), OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        TicketDetailView after = get(path, l1(), TicketDetailView.class).getBody();
        assertThat(after.getTags()).doesNotContain(tag);
    }

    private Long findTagId(String name) {
        // 通过 mapper 直接查询标签 id
        var tag = tagMapper.findByName(name);
        return tag != null ? tag.getId() : null;
    }

    @Test
    void uc09_nonStaffCannotAddTag() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC09 perm " + uniqueSuffix());
        ResponseEntity<ApiError> resp = post("/api/v1/tickets/" + ticket.getId() + "/tags",
                Map.of("tagName", "nope-" + uniqueSuffix()), user1(), ApiError.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------
    // UC-10 工单管理
    // ------------------------------------------------------------------
    @Test
    void uc10_l1AssignToSelf_l1CannotAssignToOther_l3CanAssignAnyone() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC10 assign " + uniqueSuffix());
        String path = "/api/v1/tickets/" + ticket.getId();
        Long l1Id = userId(L1_EMAILS[0]);
        Long l1bId = userId(L1_EMAILS[1]);

        assertThat(put(path + "/assign", Map.of("staffId", l1Id), l1(), OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiError> denied = put(path + "/assign", Map.of("staffId", l1bId), l1(), ApiError.class);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(put(path + "/assign", Map.of("staffId", l1bId), l3(), OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        TicketDetailView reloaded = get(path, l3(), TicketDetailView.class).getBody();
        assertThat(reloaded.getAssigneeId()).isEqualTo(l1bId);
    }

    @Test
    void uc10_unassignPermissions() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC10 unassign " + uniqueSuffix());
        String path = "/api/v1/tickets/" + ticket.getId();
        Long l1Id = userId(L1_EMAILS[0]);
        Long l1bId = userId(L1_EMAILS[1]);

        put(path + "/assign", Map.of("staffId", l1bId), l3(), OperationResult.class);

        // L1a 未接受该工单，不能解除
        assertThat(put(path + "/unassign", null, l1(), ApiError.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // L3 可以解除
        assertThat(put(path + "/unassign", null, l3(), OperationResult.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        TicketDetailView reloaded = get(path, l3(), TicketDetailView.class).getBody();
        assertThat(reloaded.getAssigneeId()).isNull();
    }

    @Test
    void uc10_changeDepartment_close_reopen_delete() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC10 lifecycle " + uniqueSuffix());
        String path = "/api/v1/tickets/" + ticket.getId();

        assertThat(put(path + "/department", Map.of("targetDepartmentId", deptBId), l2(), OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        TicketDetailView moved = get(path, l3(), TicketDetailView.class).getBody();
        assertThat(moved.getDepartmentId()).isEqualTo(deptBId);

        assertThat(put(path + "/close", null, cookie(L3_EMAILS[1]), OperationResult.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        TicketDetailView closed = get(path, l3(), TicketDetailView.class).getBody();
        assertThat(closed.getStatus()).isEqualTo("CLOSED");

        assertThat(put(path + "/reopen", null, l3(), OperationResult.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        TicketDetailView reopened = get(path, l3(), TicketDetailView.class).getBody();
        assertThat(reopened.getStatus()).isEqualTo("REOPENED");

        // L1 不能删除
        assertThat(delete(path, l1(), ApiError.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        // L3 可以删除
        assertThat(delete(path, l3(), OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(path, l3(), ApiError.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void uc10_userCanCloseOwnTicket() {
        TicketDetailView ticket = createTicket(user1(), deptAId, "UC10 user close " + uniqueSuffix());
        ResponseEntity<OperationResult> resp = put("/api/v1/tickets/" + ticket.getId() + "/close",
                null, user1(), OperationResult.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ------------------------------------------------------------------
    // UC-11 高级搜索
    // ------------------------------------------------------------------
    @Test
    void uc11_advancedSearchByTitleAndDepartment() {
        String title = "UC11 findme " + uniqueSuffix();
        TicketDetailView ticket = createTicket(user1(), deptAId, title);

        ResponseEntity<PageResult<TicketListItemView>> byTitle =
                getType("/api/v1/tickets/advanced-search?title=" + title + "&page=1&pageSize=100", l3(), TICKET_PAGE);
        assertThat(byTitle.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(byTitle.getBody().getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(byTitle.getBody().getContent()).extracting(TicketListItemView::getId).contains(ticket.getId());

        ResponseEntity<PageResult<TicketListItemView>> byDeptAndClosed =
                getType("/api/v1/tickets/advanced-search?departmentId=" + deptAId + "&closed=false&page=1&pageSize=100", l3(), TICKET_PAGE);
        assertThat(byDeptAndClosed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(byDeptAndClosed.getBody().getContent()).extracting(TicketListItemView::getId).contains(ticket.getId());

        ResponseEntity<PageResult<TicketListItemView>> byAuthor =
                getType("/api/v1/tickets/advanced-search?authorId=" + userId(USER_EMAILS[0]) + "&page=1&pageSize=100", l3(), TICKET_PAGE);
        assertThat(byAuthor.getBody().getContent()).isNotEmpty();
    }
}
