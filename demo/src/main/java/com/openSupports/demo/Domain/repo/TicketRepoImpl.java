package com.openSupports.demo.Domain.repo;

import com.openSupports.demo.Domain.Model.Tag;
import com.openSupports.demo.Domain.Model.Ticket;
import com.openSupports.demo.infra.mapper.CommentMapper;
import com.openSupports.demo.infra.mapper.TagMapper;
import com.openSupports.demo.infra.mapper.TicketMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TicketRepoImpl implements TicketRepo {

    private final TicketMapper ticketMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;

    public TicketRepoImpl(TicketMapper ticketMapper, TagMapper tagMapper, CommentMapper commentMapper) {
        this.ticketMapper = ticketMapper;
        this.tagMapper = tagMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public Ticket getById(Long ticketId) {
        Ticket ticket = ticketMapper.getById(ticketId);
        if (ticket == null) {
            return null;
        }
        ticket.setTags(tagMapper.findByTicketId(ticketId));
        ticket.setComments(commentMapper.findByTicketId(ticketId));
        return ticket;
    }

    @Override
    public List<Ticket> searchMeSent(Long userId, int offset, int size) {
        return ticketMapper.searchMeSent(userId, offset, size);
    }

    @Override
    public List<Ticket> searchMyAssigned(Long staffId, int offset, int size) {
        return ticketMapper.searchMyAssigned(staffId, offset, size);
    }

    @Override
    public List<Ticket> searchNewForDept(Long deptId, int offset, int size) {
        return ticketMapper.searchNewForDept(deptId, offset, size);
    }

    @Override
    public List<Ticket> searchAllForDept(Long deptId, String titleKeyword, Boolean closed, int offset, int size) {
        return ticketMapper.searchAllForDept(deptId, titleKeyword, closed, offset, size);
    }

    @Override
    public List<Ticket> searchByTitle(Long deptId, String titleKeyword, int offset, int size) {
        return ticketMapper.searchByTitle(deptId, titleKeyword, offset, size);
    }

    @Override
    public Ticket createAndSave(Ticket ticket, String code) {
        ticket.setTicketCode(code);
        ticketMapper.createAndSave(ticket, code);
        return ticket;
    }

    @Override
    public void update(Ticket ticket) {
        ticketMapper.update(ticket);
    }

    @Override
    public void save(Ticket ticket) {
        ticketMapper.save(ticket);
    }

    @Override
    public Tag createTag(Tag tag) {
        Tag existing = tagMapper.findByName(tag.getName());
        if (existing != null) {
            return existing;
        }
        tagMapper.insert(tag);
        return tag;
    }

    @Override
    public Tag findTagById(Long id) {
        return tagMapper.findById(id);
    }

    @Override
    public void attachTagToTicket(Long ticketId, Long tagId) {
        tagMapper.attachTagToTicket(ticketId, tagId);
    }

    @Override
    public void detachTagFromTicket(Long ticketId, Long tagId) {
        tagMapper.detachTagFromTicket(ticketId, tagId);
    }

    @Override
    public void updateUnreadStaff(Long ticketId) {
        ticketMapper.updateUnreadStaff(ticketId);
    }

    @Override
    public void assignTo(Long ticketId, Long staffId) {
        ticketMapper.assignTo(ticketId, staffId);
    }

    @Override
    public void unAssign(Long ticketId) {
        ticketMapper.unAssign(ticketId);
    }

    @Override
    public void closeTicket(Long ticketId) {
        ticketMapper.closeTicket(ticketId);
    }

    @Override
    public void reopenTicket(Long ticketId) {
        ticketMapper.reopenTicket(ticketId);
    }

    @Override
    public void deleteById(Long ticketId) {
        ticketMapper.deleteById(ticketId);
    }

    @Override
    public void changeDepartment(Long ticketId, Long newDeptId) {
        ticketMapper.changeDepartment(ticketId, newDeptId);
    }

    @Override
    public List<Ticket> advancedSearch(String title, String tags, String ticketCode, Boolean closed,
                                       LocalDateTime dateFrom, LocalDateTime dateTo, Long departmentId,
                                       Long authorId, Long ownerId, Boolean assigned, String query,
                                       String orderBy, int page, int pageSize) {
        return ticketMapper.advancedSearch(title, tags, ticketCode, closed, dateFrom, dateTo,
                departmentId, authorId, ownerId, assigned, query, orderBy, page, pageSize);
    }

    @Override
    public int countAdvancedSearch(String title, String tags, String ticketCode, Boolean closed,
                                   LocalDateTime dateFrom, LocalDateTime dateTo, Long departmentId,
                                   Long authorId, Long ownerId, Boolean assigned, String query) {
        return ticketMapper.countAdvancedSearch(title, tags, ticketCode, closed, dateFrom, dateTo,
                departmentId, authorId, ownerId, assigned, query);
    }
}
