package com.openSupports.demo.infra.mapper;
import com.openSupports.demo.Domain.Model.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface DepartmentMapper {
    Department findById(@Param("id") Long id);
    Department findByName(@Param("name") String name);
    boolean existsByName(@Param("name") String name);
    List<Department> findAll();
    int insert(Department dept);
    void updateName(@Param("id") Long id, @Param("newName") String newName);
    void deleteById(@Param("id") Long id);
    Department getDefault();
    void migrateStaff(@Param("sourceDeptId") Long sourceDeptId, @Param("targetDeptId") Long targetDeptId);
    void migrateTickets(@Param("sourceDeptId") Long sourceDeptId, @Param("targetDeptId") Long targetDeptId);
    void unAssignTicketsByStaffDept(@Param("sourceDeptId") Long sourceDeptId);
    void markDefault(@Param("id") Long id);
}