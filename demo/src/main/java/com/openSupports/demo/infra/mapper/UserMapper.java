package com.openSupports.demo.infra.mapper;

import com.openSupports.demo.Domain.Model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {
    User findByEmail(@Param("email") String email);
    User findById(@Param("id") Long id);
    int insert(User user);
    void updatePassword(User user);
    void changeEmail(@Param("id") Long id, @Param("email") String email, @Param("excludeEmail") String excludeEmail);
    void setState(@Param("id") Long id, @Param("state") String state);
    void deleteById(@Param("id") Long id);
    List<User> searchByKeyword(@Param("emailKeyword") String emailKeyword,
                               @Param("registerDateFrom") String registerDateFrom,
                               @Param("registerDateTo") String registerDateTo,
                               @Param("sortBy") String sortBy,
                               @Param("pageSize") int pageSize,
                               @Param("offset") int offset);
    int countSearchResults(@Param("emailKeyword") String emailKeyword,
                           @Param("registerDateFrom") String registerDateFrom,
                           @Param("registerDateTo") String registerDateTo);
    List<User> findAll(@Param("pageSize") int pageSize, @Param("offset") int offset);
    int countAll();
}
