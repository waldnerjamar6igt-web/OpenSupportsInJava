package com.openSupports.demo.Domain.Model;

import com.openSupports.demo.infra.exception.BusinessRuleViolationException;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单聚合根 / Ticket aggregate.
 *
 * <p>对应 BA.md UC-04 ~ UC-11。领域方法只负责状态一致性，持久化由 Repo 完成。</p>
 */
@Data
@NoArgsConstructor
public class Ticket {

    private Long id;
    private String ticketCode;
    private String title;
    private String content;
    private String status = "OPEN";
    private String priority = "NORMAL";
    private Long departmentId;
    private String departmentName;
    private Long authorId;
    private String authorEmail;
    private Long assigneeId;
    private String assigneeName;
    private Boolean unreadStaff = Boolean.TRUE;
    private Boolean closed = Boolean.FALSE;
    private Boolean editedTitle = Boolean.FALSE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActivityAt;
    private Integer unreadCount = 0;
    private List<Comment> comments = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    /** UC-04: 创建工单，作者加入工单，标记员工未读。 */
    public void createTicket(Long departmentId, Long authorId, String authorEmail) {
        this.departmentId = departmentId;
        this.authorId = authorId;
        this.authorEmail = authorEmail;
        this.status = "OPEN";
        this.closed = Boolean.FALSE;
        this.unreadStaff = Boolean.TRUE;
        this.editedTitle = Boolean.FALSE;
        this.unreadCount = 0;
        this.comments = new ArrayList<>();
        this.tags = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastActivityAt = now;
    }

    public boolean isOpen() {
        return !Boolean.TRUE.equals(this.closed);
    }

    /** UC-07: 追加评论；员工回复可私密，用户回复会重新标记员工未读。 */
    public void addComment(Comment comment, boolean byStaff) {
        if (comment.getContent() == null || comment.getContent().isBlank()) {
            throw new BusinessRuleViolationException("EMPTY_COMMENT", "评论内容不能为空");
        }
        if (!isOpen()) {
            throw new BusinessRuleViolationException("TICKET_CLOSED", "工单已关闭");
        }
        if (comments == null) {
            comments = new ArrayList<>();
        }
        comment.setTicketId(this.id);
        comment.setAuthorType(byStaff ? "STAFF" : "USER");
        comment.setIsPrivate(byStaff && Boolean.TRUE.equals(comment.getIsPrivate()));
        comment.setIsEdited(Boolean.FALSE);
        LocalDateTime now = LocalDateTime.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        comments.add(comment);
        this.lastActivityAt = now;
        if (!byStaff) {
            this.unreadStaff = Boolean.TRUE;
        }
    }

    /** UC-08: 编辑标题，置 editedTitle=true。 */
    public void editTitle(String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            throw new BusinessRuleViolationException("EMPTY_TITLE", "标题不能为空");
        }
        this.title = newTitle.trim();
        this.editedTitle = Boolean.TRUE;
        this.updatedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

    /** UC-08: 仅作者本人、且为最新一条未编辑评论可编辑。 */
    public void editLatestComment(Long commentId, String content) {
        if (!isOpen()) {
            throw new BusinessRuleViolationException("TICKET_CLOSED", "工单已关闭");
        }
        if (comments == null) {
            comments = new ArrayList<>();
        }
        Comment target = comments.stream()
                .filter(c -> commentId.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("COMMENT_NOT_FOUND", "评论不存在"));
        target.setContent(content);
        target.setIsEdited(Boolean.TRUE);
        target.setUpdatedAt(LocalDateTime.now());
        this.lastActivityAt = LocalDateTime.now();
    }

    /** UC-09: 添加标签，重复贴标抛 TAG_EXISTS。 */
    public void addTag(Tag tag) {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        boolean exists = tags.stream().anyMatch(t -> t.getName() != null
                && t.getName().equalsIgnoreCase(tag.getName()));
        if (exists) {
            throw new BusinessRuleViolationException("TAG_EXISTS", "该标签已存在");
        }
        tags.add(tag);
    }

    public void removeTag(Tag tag) {
        if (tags == null) {
            return;
        }
        tags.removeIf(t -> tag.getId() != null && tag.getId().equals(t.getId()));
    }

    /** UC-10: 分配工单。 */
    public void assignTo(Long staffId, String staffName) {
        if (!isOpen()) {
            throw new BusinessRuleViolationException("TICKET_CLOSED", "工单已关闭，无法分配");
        }
        if (staffId == null) {
            throw new BusinessRuleViolationException("INVALID_ASSIGNMENT", "被分配人不能为空");
        }
        this.assigneeId = staffId;
        this.assigneeName = staffName;
        this.unreadStaff = Boolean.FALSE;
        this.updatedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

    public void unAssign() {
        this.assigneeId = null;
        this.assigneeName = null;
        this.updatedAt = LocalDateTime.now();
    }

    /** UC-10: 变更部门，解除分配关系。 */
    public void changeDepartment(Long newDepartmentId) {
        this.departmentId = newDepartmentId;
        this.assigneeId = null;
        this.assigneeName = null;
        this.updatedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

    public void closeTicket() {
        this.closed = Boolean.TRUE;
        this.status = "CLOSED";
        this.updatedAt = LocalDateTime.now();
    }

    public void reopenTicket() {
        this.closed = Boolean.FALSE;
        this.status = "REOPENED";
        this.assigneeId = null;
        this.assigneeName = null;
        this.unreadStaff = Boolean.TRUE;
        this.updatedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

    public void delete() {
        // 删除由 Repo 完成，领域层仅作为语义标记。
        this.closed = Boolean.TRUE;
        this.status = "CLOSED";
    }

    public void markStaffRead() {
        this.unreadStaff = Boolean.FALSE;
    }
}
