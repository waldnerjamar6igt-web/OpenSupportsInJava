package com.openSupports.demo.infra.mapper;

import com.openSupports.demo.Domain.Model.Tag;
import com.openSupports.demo.Domain.Model.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface TicketMapper {
    Ticket getById(@Param("ticketId") Long ticketId);
    List<Ticket> searchMeSent(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);
    List<Ticket> searchMyAssigned(@Param("staffId") Long staffId, @Param("offset") int offset, @Param("size") int size);
    List<Ticket> searchNewForDept(@Param("deptId") Long deptId, @Param("offset") int offset, @Param("size") int size);
    List<Ticket> searchAllForDept(@Param("deptId") Long deptId, @Param("titleKeyword") String titleKeyword, @Param("closed") Boolean closed, @Param("offset") int offset, @Param("size") int size);
    List<Ticket> searchByTitle(@Param("deptId") Long deptId, @Param("titleKeyword") String titleKeyword, @Param("offset") int offset, @Param("size") int size);
    int createAndSave(Ticket ticket, @Param("code") String code);
    void update(Ticket ticket);
    void save(Ticket ticket);
    void saveBatch(Collection<Ticket> tickets);
    int insertTag(Tag tag);
    void attachTagToTicket(@Param("ticketId") Long ticketId, @Param("tagId") Long tagId);
    Tag findTagByName(@Param("name") String name);
    Tag findTagById(@Param("id") Long id);
    void detachTagFromTicket(@Param("ticketId") Long ticketId, @Param("tagId") Long tagId);
    void updateUnreadStaff(@Param("ticketId") Long ticketId);
    void assignTo(@Param("ticketId") Long ticketId, @Param("staffId") Long staffId);
    void unAssign(@Param("ticketId") Long ticketId);
    void closeTicket(@Param("ticketId") Long ticketId);
    void reopenTicket(@Param("ticketId") Long ticketId);
    void deleteById(@Param("ticketId") Long ticketId);
    void changeDepartment(@Param("ticketId") Long ticketId, @Param("newDeptId") Long newDeptId);
    void unAssignTicketsByAuthor(@Param("authorId") Long authorId);
    void closeTicketsByAuthor(@Param("authorId") Long authorId);
    List<Ticket> advancedSearch(@Param("title") String title, @Param("tags") String tags, @Param("ticketCode") String ticketCode,
                                 @Param("closed") Boolean closed, @Param("dateFrom") LocalDateTime dateFrom,
                                 @Param("dateTo") LocalDateTime dateTo, @Param("departmentId") Long departmentId,
                                 @Param("authorId") Long authorId, @Param("ownerId") Long ownerId,
                                 @Param("assigned") Boolean assigned, @Param("query") String query,
                                 @Param("orderBy") String orderBy, @Param("page") int page, @Param("pageSize") int pageSize);
    int countAdvancedSearch(@Param("title") String title, @Param("tags") String tags, @Param("ticketCode") String ticketCode,
                             @Param("closed") Boolean closed, @Param("dateFrom") LocalDateTime dateFrom,
                             @Param("dateTo") LocalDateTime dateTo, @Param("departmentId") Long departmentId,
                             @Param("authorId") Long authorId, @Param("ownerId") Long ownerId,
                             @Param("assigned") Boolean assigned, @Param("query") String query);
}
