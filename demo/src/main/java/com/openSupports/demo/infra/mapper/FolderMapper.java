package com.openSupports.demo.infra.mapper;
import com.openSupports.demo.Domain.Model.Folder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface FolderMapper {
    List<Folder> findAllPublic();
    List<Folder> findAllIncludingPrivate();
    Folder findById(@Param("id") Long id);
    int insert(Folder folder);
    void updateName(@Param("id") Long id, @Param("name") String name);
    void updateState(@Param("id") Long id, @Param("isPrivate") boolean isPrivate);
    void reorder(@Param("ids") List<Long> ids);
    void remove(@Param("id") Long id);
    void moveArticlesToNullFolder(@Param("folderId") Long folderId);
}