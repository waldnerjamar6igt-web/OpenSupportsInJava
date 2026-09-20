package com.openSupports.demo.infra.mapper;

import com.openSupports.demo.Domain.Model.Staff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface StafaMapper {
    Staff findByEmail(@Param("email") String email);
    Staff findById(@Param("id") Long id);
    Staff findByUserId(@Param("userId") Long userId);
    int insertStaff(Staff staff);
    void updatePassword(Staff staff);
    void changeDepartment(@Param("id") Long id, @Param("deptId") Long deptId);
    void setState(@Param("id") Long id, @Param("state") String state);
    void deleteById(@Param("id") Long id);
    List<Staff> searchWithFilters(@Param("emailKeyword") String emailKeyword,
                                  @Param("departmentId") Long departmentId,
                                  @Param("level") Integer level,
                                  @Param("registerDateFrom") String registerDateFrom,
                                  @Param("registerDateTo") String registerDateTo,
                                  @Param("sortBy") String sortBy,
                                  @Param("pageSize") int pageSize,
                                  @Param("offset") int offset);
    int countSearchResults(@Param("emailKeyword") String emailKeyword,
                           @Param("departmentId") Long departmentId,
                           @Param("level") Integer level,
                           @Param("registerDateFrom") String registerDateFrom,
                           @Param("registerDateTo") String registerDateTo);
    List<Staff> findAll(@Param("pageSize") int pageSize, @Param("offset") int offset);
    int countAll();
    boolean existsByEmail(@Param("email") String email);
    void unAssignAllTickets(@Param("staffId") Long staffId);
    void closeAllTickets(@Param("staffId") Long staffId);
    void updateLevel(@Param("id") Long id, @Param("level") Integer level);
}
