package com.openSupports.demo.infra.mapper;
import com.openSupports.demo.Domain.Model.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface ArticleMapper {
    Article getById(@Param("id") Long id);
    List<Article> getByFolderId(@Param("folderId") Long folderId, @Param("includePrivate") boolean includePrivate);
    int createAndSave(Article article);
    void update(Article article);
    void deleteById(@Param("id") Long id);
}