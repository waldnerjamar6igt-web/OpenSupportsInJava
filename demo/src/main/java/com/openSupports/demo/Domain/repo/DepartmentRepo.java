package com.openSupports.demo.Domain.repo;

import com.openSupports.demo.Domain.Model.Department;
import java.util.List;

public interface DepartmentRepo {
    Department findById(Long id);
    Department findByName(String name);
    boolean existsByName(String name);
    List<Department> findAll();
    int insert(Department dept);
    void updateName(Long id, String newName);
    void deleteById(Long id);
    Department getDefault();
    void migrateStaff(Long sourceDeptId, Long targetDeptId);
    void migrateTickets(Long sourceDeptId, Long targetDeptId);
    void markDefault(Long id);
}
