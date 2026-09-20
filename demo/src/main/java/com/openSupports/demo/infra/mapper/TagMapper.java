package com.openSupports.demo.infra.mapper;

import com.openSupports.demo.Domain.Model.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TagMapper {
    Tag findByName(@Param("name") String name);
    Tag findById(@Param("id") Long id);
    int insert(Tag tag);
    void deleteById(@Param("id") Long id);
    void deleteByTicketId(@Param("ticketId") Long ticketId);
    List<Tag> findByTicketId(@Param("ticketId") Long ticketId);
    boolean existsInTicket(@Param("ticketId") Long ticketId, @Param("tagName") String tagName);
    void attachTagToTicket(@Param("ticketId") Long ticketId, @Param("tagId") Long tagId);
    void detachTagFromTicket(@Param("ticketId") Long ticketId, @Param("tagId") Long tagId);
}
