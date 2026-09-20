package com.openSupports.demo.api.controller;

import com.openSupports.demo.api.dto.common.OperationResult;
import com.openSupports.demo.api.dto.common.PageResult;
import com.openSupports.demo.api.dto.ticket.*;
import com.openSupports.demo.application.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public TicketDetailView createTicket(@Valid @RequestBody CreateTicketCmd cmd, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ticketService.createTicket(userId, cmd);
    }

    @GetMapping("/me-sent")
    public PageResult<TicketListItemView> getMySentTickets(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ticketService.getMySentTickets(userId, page, size);
    }

    @GetMapping("/me-assigned")
    public PageResult<TicketListItemView> getMyAssignedTickets(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Long staffId = (Long) request.getAttribute("currentUserId");
        return ticketService.getMyAssignedTickets(staffId, page, size);
    }

    @GetMapping("/new")
    public PageResult<TicketListItemView> getNewTickets(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Long deptId = (Long) request.getAttribute("currentUserDeptId");
        return ticketService.getNewTicketsForDept(deptId, page, size);
    }

    @GetMapping("/all")
    public PageResult<TicketListItemView> getAllTickets(HttpServletRequest request,
            @RequestParam(required = false) String titleKeyword,
            @RequestParam(required = false) Boolean closed,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Long deptId = (Long) request.getAttribute("currentUserDeptId");
        return ticketService.getAllTicketsForDept(deptId, titleKeyword, closed, page, size);
    }

    @GetMapping("/search")
    public PageResult<TicketListItemView> searchByTitle(HttpServletRequest request,
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Long deptId = (Long) request.getAttribute("currentUserDeptId");
        return ticketService.searchByTitle(deptId, q, page, size);
    }

    @GetMapping("/{id}")
    public TicketDetailView getTicketDetail(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        return ticketService.getTicketDetail(id, currentUserId);
    }

    @PostMapping("/{id}/comments")
    public CommentView addComment(@PathVariable Long id, @Valid @RequestBody AddCommentCmd cmd, HttpServletRequest request) {
        Long authorId = (Long) request.getAttribute("currentUserId");
        return ticketService.addComment(id, authorId, cmd);
    }

    @PutMapping("/{id}/title")
    public OperationResult editTitle(@PathVariable Long id, @Valid @RequestBody EditTicketTitleCmd cmd, HttpServletRequest request) {
        Long actorId = (Long) request.getAttribute("currentUserId");
        ticketService.editTitle(id, actorId, cmd);
        return new OperationResult(true, "标题已更新");
    }

    @PutMapping("/comments/{commentId}")
    public OperationResult editComment(@PathVariable Long commentId, @Valid @RequestBody EditCommentCmd cmd, HttpServletRequest request) {
        Long actorId = (Long) request.getAttribute("currentUserId");
        ticketService.editComment(commentId, actorId, cmd);
        return new OperationResult(true, "评论已更新");
    }

    @PostMapping("/{id}/tags")
    public OperationResult addTag(@PathVariable Long id, @Valid @RequestBody AddTagCmd cmd, HttpServletRequest request) {
        Long staffId = (Long) request.getAttribute("currentUserId");
        ticketService.addTag(id, staffId, cmd);
        return new OperationResult(true, "标签已添加");
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public OperationResult removeTag(@PathVariable Long id, @PathVariable Long tagId) {
        ticketService.removeTag(id, tagId);
        return new OperationResult(true, "标签已移除");
    }

    @PutMapping("/{id}/assign")
    public OperationResult assignTicket(@PathVariable Long id, @Valid @RequestBody AssignTicketCmd cmd, HttpServletRequest request) {
        Long staffId = (Long) request.getAttribute("currentUserId");
        ticketService.assignTicket(id, staffId, cmd);
        return new OperationResult(true, "工单已分配");
    }

    @PutMapping("/{id}/unassign")
    public OperationResult unassignTicket(@PathVariable Long id, HttpServletRequest request) {
        Long actorId = (Long) request.getAttribute("currentUserId");
        ticketService.unassignTicket(id, actorId);
        return new OperationResult(true, "工单已取消分配");
    }

    @PutMapping("/{id}/department")
    public OperationResult changeDepartment(@PathVariable Long id, @Valid @RequestBody ChangeTicketDeptCmd cmd, HttpServletRequest request) {
        Long actorId = (Long) request.getAttribute("currentUserId");
        ticketService.changeDepartment(id, actorId, cmd);
        return new OperationResult(true, "部门已变更");
    }

    @PutMapping("/{id}/close")
    public OperationResult closeTicket(@PathVariable Long id, HttpServletRequest request) {
        Long actorId = (Long) request.getAttribute("currentUserId");
        ticketService.closeTicket(id, actorId);
        return new OperationResult(true, "工单已关闭");
    }

    @PutMapping("/{id}/reopen")
    public OperationResult reopenTicket(@PathVariable Long id, HttpServletRequest request) {
        Long actorId = (Long) request.getAttribute("currentUserId");
        ticketService.reopenTicket(id, actorId);
        return new OperationResult(true, "工单已重开");
    }

    @DeleteMapping("/{id}")
    public OperationResult deleteTicket(@PathVariable Long id, HttpServletRequest request) {
        Long actorId = (Long) request.getAttribute("currentUserId");
        ticketService.deleteTicket(id, actorId);
        return new OperationResult(true, "工单已删除");
    }

    @GetMapping("/advanced-search")
    public PageResult<TicketListItemView> advancedSearch(AdvancedSearchCmd cmd) {
        return ticketService.advancedSearch(cmd);
    }
}