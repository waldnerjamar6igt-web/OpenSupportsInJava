package com.openSupports.demo.Domain.service;

import com.openSupports.demo.Domain.Model.Account;
import com.openSupports.demo.Domain.Model.Ticket;
import com.openSupports.demo.infra.exception.BusinessRuleViolationException;
import com.openSupports.demo.infra.exception.PermissionDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TicketDomainService {

    public void canCloseTicket(Ticket ticket, Account account) {
        if (!account.isEnabled()) throw new BusinessRuleViolationException("ACCOUNT_DISABLED", "账户已停用");
        // User can close own tickets
        if (!account.isStaff() && account.getId().equals(ticket.getAuthorId())) return;
        // Staff can close tickets in their department or assigned
        if (account.isAdmin()) return;
        if (Integer.valueOf(1).equals(account.getLevel()) || Integer.valueOf(2).equals(account.getLevel())) {
            if (ticket.getAssigneeId() != null && ticket.getAssigneeId().equals(account.getId())) return;
            if (ticket.getDepartmentId() != null && ticket.getDepartmentId().equals(account.getDepartmentId())) return;
        }
        throw new PermissionDeniedException("无权关闭该工单");
    }

    public void canReOpenTicket(Ticket ticket, Account account) {
        if (!account.isEnabled()) throw new BusinessRuleViolationException("ACCOUNT_DISABLED", "账户已停用");
        if (!Boolean.TRUE.equals(ticket.getClosed())) throw new BusinessRuleViolationException("TICKET_NOT_CLOSED", "工单未关闭");
        // User can reopen own closed tickets
        if (!account.isStaff() && account.getId().equals(ticket.getAuthorId())) return;
        // Staff same rules as closing
        if (account.isAdmin()) return;
        if (Integer.valueOf(1).equals(account.getLevel()) || Integer.valueOf(2).equals(account.getLevel())) {
            if (ticket.getDepartmentId() != null && ticket.getDepartmentId().equals(account.getDepartmentId())) return;
        }
        throw new PermissionDeniedException("无权重新打开该工单");
    }

    public void canDeleteTicket(Ticket ticket, Account account) {
        if (!account.isEnabled()) throw new BusinessRuleViolationException("ACCOUNT_DISABLED", "账户已停用");
        if (!account.isAdmin()) throw new PermissionDeniedException("只有管理员可以删除工单");
    }

    public void canManageTicket(Ticket ticket, Account account) {
        if (!account.isEnabled()) throw new BusinessRuleViolationException("ACCOUNT_DISABLED", "账户已停用");
        // Admin can manage any ticket
        if (account.isAdmin()) return;
        // L1/L2 staff can manage tickets in their department
        if (Integer.valueOf(1).equals(account.getLevel()) || Integer.valueOf(2).equals(account.getLevel())) {
            if (ticket.getDepartmentId() != null && ticket.getDepartmentId().equals(account.getDepartmentId())) return;
        }
        throw new PermissionDeniedException("无权管理该工单");
    }

    public void canCreateTicket(Account account) {
        if (!account.isEnabled()) throw new BusinessRuleViolationException("ACCOUNT_DISABLED", "账户已停用");
    }
}
