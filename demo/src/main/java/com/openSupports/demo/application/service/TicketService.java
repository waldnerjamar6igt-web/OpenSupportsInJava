package com.openSupports.demo.application.service;

import com.openSupports.demo.Domain.Model.Account;
import com.openSupports.demo.Domain.Model.Comment;
import com.openSupports.demo.Domain.Model.Tag;
import com.openSupports.demo.Domain.Model.Ticket;
import com.openSupports.demo.Domain.repo.AccountRepo;
import com.openSupports.demo.Domain.repo.DepartmentRepo;
import com.openSupports.demo.Domain.repo.TicketRepo;
import com.openSupports.demo.Domain.service.TicketDomainService;
import com.openSupports.demo.api.dto.common.PageResult;
import com.openSupports.demo.api.dto.ticket.*;
import com.openSupports.demo.infra.exception.BusinessRuleViolationException;
import com.openSupports.demo.infra.exception.PermissionDeniedException;
import com.openSupports.demo.infra.exception.ResourceNotFoundException;
import com.openSupports.demo.infra.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepo ticketRepo;
    private final AccountRepo accountRepo;
    private final DepartmentRepo departmentRepo;
    private final TicketDomainService ticketDomainService;
    private final CommentMapper commentMapper;

    // ------------------------------------------------------------------
    // UC-04 创建工单
    // ------------------------------------------------------------------
    @Transactional
    public TicketDetailView createTicket(Long authorId, CreateTicketCmd cmd) {
        Account author = accountRepo.findById(authorId);
        if (author == null || !author.isEnabled()) {
            throw new ResourceNotFoundException("Author", authorId);
        }
        var dept = departmentRepo.findById(cmd.getDepartmentId());
        if (dept == null) {
            throw new ResourceNotFoundException("Department", cmd.getDepartmentId());
        }
        if (Boolean.TRUE.equals(dept.getIsDefault())) {
            throw new BusinessRuleViolationException("INVALID_DEPARTMENT", "不能选择默认部门");
        }

        Ticket ticket = new Ticket();
        ticket.createTicket(cmd.getDepartmentId(), authorId, author.getEmail());
        ticket.setTitle(cmd.getTitle());
        ticket.setContent(cmd.getContent());
        if (cmd.getPriority() != null) {
            ticket.setPriority(cmd.getPriority());
        }
        ticketRepo.createAndSave(ticket, generateTicketCode());

        Ticket saved = ticketRepo.getById(ticket.getId());
        return toDetailView(saved != null ? saved : ticket, false, false);
    }

    // ------------------------------------------------------------------
    // UC-05 工单粗略视图
    // ------------------------------------------------------------------
    public PageResult<TicketListItemView> getMySentTickets(Long userId, int page, int size) {
        List<Ticket> tickets = ticketRepo.searchMeSent(userId, (page - 1) * size, size);
        long total = ticketRepo.countAdvancedSearch(null, null, null, null, null, null,
                null, userId, null, null, null);
        return wrapList(tickets, total, page, size);
    }

    public PageResult<TicketListItemView> getMyAssignedTickets(Long staffId, int page, int size) {
        List<Ticket> tickets = ticketRepo.searchMyAssigned(staffId, (page - 1) * size, size);
        long total = ticketRepo.countAdvancedSearch(null, null, null, null, null, null,
                null, null, staffId, true, null);
        return wrapList(tickets, total, page, size);
    }

    public PageResult<TicketListItemView> getNewTicketsForDept(Long deptId, int page, int size) {
        List<Ticket> tickets = ticketRepo.searchNewForDept(deptId, (page - 1) * size, size);
        long total = ticketRepo.countAdvancedSearch(null, null, null, false, null, null,
                deptId, null, null, false, null);
        return wrapList(tickets, total, page, size);
    }

    public PageResult<TicketListItemView> getAllTicketsForDept(Long deptId, String titleKeyword, Boolean closed, int page, int size) {
        List<Ticket> tickets = ticketRepo.searchAllForDept(deptId, titleKeyword, closed, (page - 1) * size, size);
        long total = ticketRepo.countAdvancedSearch(titleKeyword, null, null, closed, null, null,
                deptId, null, null, null, null);
        return wrapList(tickets, total, page, size);
    }

    public PageResult<TicketListItemView> searchByTitle(Long deptId, String titleKeyword, int page, int size) {
        List<Ticket> tickets = ticketRepo.searchByTitle(deptId, titleKeyword, (page - 1) * size, size);
        long total = ticketRepo.countAdvancedSearch(titleKeyword, null, null, null, null, null,
                deptId, null, null, null, null);
        return wrapList(tickets, total, page, size);
    }

    // ------------------------------------------------------------------
    // UC-06 工单具体视图
    // ------------------------------------------------------------------
    @Transactional
    public TicketDetailView getTicketDetail(Long ticketId, Long currentUserId) {
        Ticket ticket = ticketRepo.getById(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        Account account = currentUserId != null ? accountRepo.findById(currentUserId) : null;
        if (!canAccess(ticket, account)) {
            throw new PermissionDeniedException("无权查看该工单");
        }
        boolean staff = account != null && account.isStaff();
        if (staff && Boolean.TRUE.equals(ticket.getUnreadStaff())) {
            ticketRepo.updateUnreadStaff(ticketId);
        }
        return toDetailView(ticket, true, staff);
    }

    // ------------------------------------------------------------------
    // UC-07 回复工单
    // ------------------------------------------------------------------
    @Transactional
    public CommentView addComment(Long ticketId, Long authorId, AddCommentCmd cmd) {
        Ticket ticket = ticketRepo.getById(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        Account account = accountRepo.findById(authorId);
        if (account == null) {
            throw new ResourceNotFoundException("Account", authorId);
        }
        boolean isStaff = account.isStaff();
        if (!isStaff && !authorId.equals(ticket.getAuthorId())) {
            throw new PermissionDeniedException("无权回复该工单");
        }
        Comment comment = new Comment();
        comment.setContent(cmd.getContent());
        comment.setIsPrivate(Boolean.TRUE.equals(cmd.getPrivateNote()));
        comment.setAuthorId(authorId);
        ticket.addComment(comment, isStaff);
        commentMapper.insert(comment);
        ticketRepo.update(ticket);
        return toCommentView(comment);
    }

    // ------------------------------------------------------------------
    // UC-08 编辑工单
    // ------------------------------------------------------------------
    @Transactional
    public void editTitle(Long ticketId, Long actorId, EditTicketTitleCmd cmd) {
        Ticket ticket = ticketRepo.getById(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        Account account = accountRepo.findById(actorId);
        if (!canManageTicket(ticket, account)) {
            throw new PermissionDeniedException("无权编辑该工单");
        }
        ticket.editTitle(cmd.getNewTitle());
        ticketRepo.update(ticket);
    }

    @Transactional
    public void editComment(Long commentId, Long actorId, EditCommentCmd cmd) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new ResourceNotFoundException("Comment", commentId);
        }
        Ticket ticket = ticketRepo.getById(comment.getTicketId());
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", comment.getTicketId());
        }
        if (!ticket.isOpen()) {
            throw new BusinessRuleViolationException("TICKET_CLOSED", "工单已关闭");
        }
        if (!actorId.equals(comment.getAuthorId())) {
            throw new PermissionDeniedException("只有评论作者可以编辑");
        }
        if (!commentMapper.isLatestComment(comment.getTicketId(), actorId, commentId)) {
            throw new BusinessRuleViolationException("NOT_LATEST_COMMENT", "只能编辑自己最新的一条评论");
        }
        comment.setContent(cmd.getContent());
        commentMapper.updateContent(comment);
        commentMapper.updateEdited(commentId, actorId);
        ticketRepo.save(ticket);
    }

    // ------------------------------------------------------------------
    // UC-09 工单标签
    // ------------------------------------------------------------------
    @Transactional
    public void addTag(Long ticketId, Long staffId, AddTagCmd cmd) {
        Ticket ticket = ticketRepo.getById(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        Account account = accountRepo.findById(staffId);
        if (!canManageTicket(ticket, account) || account == null || !account.isStaff()) {
            throw new PermissionDeniedException("无权管理标签");
        }
        Tag tag = new Tag();
        tag.setName(cmd.getTagName().trim());
        Tag existing = ticketRepo.createTag(tag);
        ticket.addTag(existing);
        ticketRepo.attachTagToTicket(ticketId, existing.getId());
    }

    @Transactional
    public void removeTag(Long ticketId, Long tagId) {
        Ticket ticket = ticketRepo.getById(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        ticketRepo.detachTagFromTicket(ticketId, tagId);
    }

    // ------------------------------------------------------------------
    // UC-10 工单管理
    // ------------------------------------------------------------------
    @Transactional
    public void assignTicket(Long ticketId, Long currentStaffId, AssignTicketCmd cmd) {
        Ticket ticket = ticketRepo.getById(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        Account actor = accountRepo.findById(currentStaffId);
        if (actor == null || !actor.isStaff()) {
            throw new PermissionDeniedException("只有员工可以分配工单");
        }
        Long targetId = cmd.getStaffId() != null ? cmd.getStaffId() : currentStaffId;
        if (!actor.isAdmin() && !targetId.equals(currentStaffId)) {
            throw new PermissionDeniedException("L1/L2 只能将工单分配给自己");
        }
        if (!actor.isAdmin() && !ticket.getDepartmentId().equals(actor.getDepartmentId())) {
            throw new PermissionDeniedException("只能分配本部门工单");
        }
        Account target = accountRepo.findById(targetId);
        if (target == null || !target.isStaff()) {
            throw new ResourceNotFoundException("Staff", targetId);
        }
        ticket.assignTo(targetId, target.getEmail());
        ticketRepo.update(ticket);
    }

    @Transactional
    public void unassignTicket(Long ticketId, Long actorId) {
        Ticket ticket = ticketRepo.getById(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        Account actor = accountRepo.findById(actorId);
        if (actor == null || !actor.isStaff()) {
            throw new PermissionDeniedException("只有员工可以取消分配");
        }
        if (!actor.isAdmin() && !actorId.equals(ticket.getAssigneeId())) {
            throw new PermissionDeniedException("只能解除分配给自己接受的工单");
        }
        ticket.unAssign();
        ticketRepo.update(ticket);
    }

    @Transactional
    public void changeDepartment(Long ticketId, Long actorId, ChangeTicketDeptCmd cmd) {
        Ticket ticket = ticketRepo.getById(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        Account actor = accountRepo.findById(actorId);
        if (!canManageTicket(ticket, actor)) {
            throw new PermissionDeniedException("无权变更工单部门");
        }
        var target = departmentRepo.findById(cmd.getTargetDepartmentId());
        if (target == null) {
            throw new ResourceNotFoundException("Department", cmd.getTargetDepartmentId());
        }
        if (Boolean.TRUE.equals(target.getIsDefault())) {
            throw new BusinessRuleViolationException("INVALID_DEPARTMENT", "不能迁移到默认部门");
        }
        ticket.changeDepartment(cmd.getTargetDepartmentId());
        ticketRepo.update(ticket);
    }

    @Transactional
    public void closeTicket(Long ticketId, Long actorId) {
        Ticket ticket = ticketRepo.getById(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        ticketDomainService.canCloseTicket(ticket, accountRepo.findById(actorId));
        ticket.closeTicket();
        ticketRepo.update(ticket);
    }

    @Transactional
    public void reopenTicket(Long ticketId, Long actorId) {
        Ticket ticket = ticketRepo.getById(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        ticketDomainService.canReOpenTicket(ticket, accountRepo.findById(actorId));
        ticket.reopenTicket();
        ticketRepo.update(ticket);
    }

    @Transactional
    public void deleteTicket(Long ticketId, Long actorId) {
        Ticket ticket = ticketRepo.getById(ticketId);
        if (ticket == null) {
            throw new ResourceNotFoundException("Ticket", ticketId);
        }
        ticketDomainService.canDeleteTicket(ticket, accountRepo.findById(actorId));
        ticketRepo.deleteById(ticketId);
    }

    // ------------------------------------------------------------------
    // UC-11 高级搜索
    // ------------------------------------------------------------------
    public PageResult<TicketListItemView> advancedSearch(AdvancedSearchCmd cmd) {
        int page = cmd.getPage() != null ? cmd.getPage() : 1;
        int size = cmd.getPageSize() != null ? cmd.getPageSize() : 10;
        LocalDateTime from = cmd.getDateFrom() != null ? cmd.getDateFrom().atStartOfDay() : null;
        LocalDateTime to = cmd.getDateTo() != null ? cmd.getDateTo().plusDays(1).atStartOfDay() : null;
        String orderBy = cmd.getOrderBy() != null && !cmd.getOrderBy().isBlank() ? cmd.getOrderBy() : "last_activity_at DESC";

        List<Ticket> tickets = ticketRepo.advancedSearch(cmd.getTitle(), cmd.getTags(), cmd.getTicketCode(),
                cmd.getClosed(), from, to, cmd.getDepartmentId(), cmd.getAuthorId(), cmd.getOwnerId(),
                cmd.getAssigned(), cmd.getQuery(), orderBy, page, size);
        int total = ticketRepo.countAdvancedSearch(cmd.getTitle(), cmd.getTags(), cmd.getTicketCode(),
                cmd.getClosed(), from, to, cmd.getDepartmentId(), cmd.getAuthorId(), cmd.getOwnerId(),
                cmd.getAssigned(), cmd.getQuery());
        return wrapList(tickets, total, page, size);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private TicketDetailView toDetailView(Ticket ticket, boolean includeComments, boolean viewerIsStaff) {
        TicketDetailView view = new TicketDetailView();
        view.setId(ticket.getId());
        view.setTicketCode(ticket.getTicketCode());
        view.setTitle(ticket.getTitle());
        view.setContent(ticket.getContent());
        view.setStatus(ticket.getStatus());
        view.setPriority(ticket.getPriority());
        view.setDepartmentName(ticket.getDepartmentName());
        view.setDepartmentId(ticket.getDepartmentId());
        view.setAssigneeName(ticket.getAssigneeName());
        view.setAssigneeId(ticket.getAssigneeId());
        view.setAuthorId(ticket.getAuthorId());
        view.setAuthorEmail(ticket.getAuthorEmail());
        view.setCreatedAt(ticket.getCreatedAt());
        view.setUpdatedAt(ticket.getUpdatedAt());
        view.setLastActivityAt(ticket.getLastActivityAt());
        view.setEditedTitle(Boolean.TRUE.equals(ticket.getEditedTitle()));

        List<CommentView> comments = new ArrayList<>();
        if (includeComments && ticket.getComments() != null) {
            for (Comment c : ticket.getComments()) {
                if (Boolean.TRUE.equals(c.getIsPrivate()) && !viewerIsStaff) {
                    continue;
                }
                comments.add(toCommentView(c));
            }
        }
        view.setComments(comments);

        List<String> tags = new ArrayList<>();
        if (ticket.getTags() != null) {
            ticket.getTags().forEach(t -> tags.add(t.getName()));
        }
        view.setTags(tags);
        return view;
    }

    private CommentView toCommentView(Comment comment) {
        CommentView view = new CommentView();
        view.setId(comment.getId());
        view.setContent(comment.getContent());
        view.setAuthorId(comment.getAuthorId());
        view.setAuthorType(comment.getAuthorType());
        view.setAuthorName(comment.getAuthorName());
        view.setCreatedAt(comment.getCreatedAt());
        view.setUpdatedAt(comment.getUpdatedAt());
        view.setPrivateNote(Boolean.TRUE.equals(comment.getIsPrivate()));
        view.setEdited(Boolean.TRUE.equals(comment.getIsEdited()));
        return view;
    }

    private PageResult<TicketListItemView> wrapList(List<Ticket> tickets, long total, int page, int size) {
        List<TicketListItemView> views = new ArrayList<>();
        if (tickets != null) {
            for (Ticket t : tickets) {
                TicketListItemView v = new TicketListItemView();
                v.setId(t.getId());
                v.setTicketCode(t.getTicketCode());
                v.setTitle(t.getTitle());
                v.setStatus(t.getStatus());
                v.setPriority(t.getPriority());
                v.setDepartmentName(t.getDepartmentName());
                v.setAssigneeName(t.getAssigneeName());
                v.setAuthorId(t.getAuthorId());
                v.setAuthorEmail(t.getAuthorEmail());
                v.setCreatedAt(t.getCreatedAt());
                v.setLastActivityAt(t.getLastActivityAt());
                v.setEditedTitle(Boolean.TRUE.equals(t.getEditedTitle()));
                v.setUnreadCount(t.getUnreadCount() != null ? t.getUnreadCount() : 0);
                views.add(v);
            }
        }
        return PageResult.of(views, total, page, size);
    }

    private String generateTicketCode() {
        return "TK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private boolean canAccess(Ticket ticket, Account account) {
        if (account == null) {
            return false;
        }
        if (account.getId().equals(ticket.getAuthorId())) {
            return true;
        }
        if (!account.isStaff()) {
            return false;
        }
        return account.isAdmin() || (ticket.getDepartmentId() != null
                && ticket.getDepartmentId().equals(account.getDepartmentId()));
    }

    private boolean canManageTicket(Ticket ticket, Account account) {
        if (account == null) {
            return false;
        }
        if (account.getId().equals(ticket.getAuthorId())) {
            return true;
        }
        if (!account.isStaff()) {
            return false;
        }
        return account.isAdmin() || (ticket.getDepartmentId() != null
                && ticket.getDepartmentId().equals(account.getDepartmentId()));
    }
}
