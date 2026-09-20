package com.openSupports.demo.Domain.repo;

import com.openSupports.demo.Domain.Model.Department;
import com.openSupports.demo.infra.mapper.DepartmentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DepartmentRepoImpl implements DepartmentRepo {

    private final DepartmentMapper mapper;

    public DepartmentRepoImpl(DepartmentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Department findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public Department findByName(String name) {
        return mapper.findByName(name);
    }

    @Override
    public boolean existsByName(String name) {
        return mapper.existsByName(name);
    }

    @Override
    public List<Department> findAll() {
        return mapper.findAll();
    }

    @Override
    public int insert(Department dept) {
        return mapper.insert(dept);
    }

    @Override
    public void updateName(Long id, String newName) {
        mapper.updateName(id, newName);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Department getDefault() {
        return mapper.getDefault();
    }

    @Override
    public void migrateStaff(Long sourceDeptId, Long targetDeptId) {
        // 迁移前解除这些员工名下的工单分配关系（UC-18）
        mapper.unAssignTicketsByStaffDept(sourceDeptId);
        mapper.migrateStaff(sourceDeptId, targetDeptId);
    }

    @Override
    public void migrateTickets(Long sourceDeptId, Long targetDeptId) {
        mapper.migrateTickets(sourceDeptId, targetDeptId);
    }

    @Override
    public void markDefault(Long id) {
        mapper.markDefault(id);
    }
}
