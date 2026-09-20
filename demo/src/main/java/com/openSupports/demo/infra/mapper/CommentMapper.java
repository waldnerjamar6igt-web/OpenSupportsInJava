package com.openSupports.demo.infra.mapper;

import com.openSupports.demo.Domain.Model.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CommentMapper {
    Comment findById(@Param("id") Long id);
    int insert(Comment comment);
    void updateContent(Comment comment);
    List<Comment> findByTicketId(@Param("ticketId") Long ticketId);
    boolean isLatestComment(@Param("ticketId") Long ticketId, @Param("authorId") Long authorId, @Param("commentId") Long commentId);
    void updateEdited(@Param("commentId") Long commentId, @Param("editedBy") Long editedBy);
}
