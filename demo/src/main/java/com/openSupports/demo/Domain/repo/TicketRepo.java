package com.openSupports.demo.Domain.repo;

import com.openSupports.demo.Domain.Model.Tag;
import com.openSupports.demo.Domain.Model.Ticket;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepo {
    Ticket getById(Long ticketId);

    List<Ticket> searchMeSent(Long userId, int offset, int size);

    List<Ticket> searchMyAssigned(Long staffId, int offset, int size);

    List<Ticket> searchNewForDept(Long deptId, int offset, int size);

    List<Ticket> searchAllForDept(Long deptId, String titleKeyword, Boolean closed, int offset, int size);

    List<Ticket> searchByTitle(Long deptId, String titleKeyword, int offset, int size);

    Ticket createAndSave(Ticket ticket, String code);

    void update(Ticket ticket);

    void save(Ticket ticket);

    /** 创建标签（若已存在则复用）。 */
    Tag createTag(Tag tag);

    Tag findTagById(Long id);

    void attachTagToTicket(Long ticketId, Long tagId);

    void detachTagFromTicket(Long ticketId, Long tagId);

    void updateUnreadStaff(Long ticketId);

    void assignTo(Long ticketId, Long staffId);

    void unAssign(Long ticketId);

    void closeTicket(Long ticketId);

    void reopenTicket(Long ticketId);

    void deleteById(Long ticketId);

    void changeDepartment(Long ticketId, Long newDeptId);

    List<Ticket> advancedSearch(String title, String tags, String ticketCode, Boolean closed,
                                LocalDateTime dateFrom, LocalDateTime dateTo, Long departmentId,
                                Long authorId, Long ownerId, Boolean assigned, String query,
                                String orderBy, int page, int pageSize);

    int countAdvancedSearch(String title, String tags, String ticketCode, Boolean closed,
                            LocalDateTime dateFrom, LocalDateTime dateTo, Long departmentId,
                            Long authorId, Long ownerId, Boolean assigned, String query);
}
