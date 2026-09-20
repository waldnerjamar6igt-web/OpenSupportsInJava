package com.openSupports.demo.application.service;

import com.openSupports.demo.Domain.Model.Account;
import com.openSupports.demo.Domain.Model.Department;
import com.openSupports.demo.Domain.repo.AccountRepo;
import com.openSupports.demo.Domain.repo.DepartmentRepo;
import com.openSupports.demo.api.dto.department.*;
import com.openSupports.demo.infra.exception.BusinessRuleViolationException;
import com.openSupports.demo.infra.exception.PermissionDeniedException;
import com.openSupports.demo.infra.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepo departmentRepo;
    private final AccountRepo accountRepo;

    public List<DepartmentOverviewView> listDepartments() {
        return departmentRepo.findAll().stream().map(this::toOverview).toList();
    }

    @Transactional
    public void createDept(Long actorId, CreateDeptCmd cmd) {
        requireAdmin(actorId);
        if (departmentRepo.existsByName(cmd.getName())) {
            throw new BusinessRuleViolationException("DEPT_NAME_DUPLICATE", "部门名称已存在");
        }
        Department dept = new Department();
        dept.setName(cmd.getName());
        dept.setIsDefault(false);
        dept.setIsPrivate(false);
        departmentRepo.insert(dept);
    }

    @Transactional
    public void updateDeptName(Long actorId, Long id, UpdateDeptNameCmd cmd) {
        requireAdmin(actorId);
        Department dept = departmentRepo.findById(id);
        if (dept == null) {
            throw new ResourceNotFoundException("Department", id);
        }
        if (Boolean.TRUE.equals(dept.getIsDefault())) {
            throw new BusinessRuleViolationException("CANNOT_RENAME_DEFAULT_DEPT", "不能重命名默认部门");
        }
        if (departmentRepo.existsByName(cmd.getNewName())) {
            throw new BusinessRuleViolationException("DEPT_NAME_DUPLICATE", "部门名称已存在");
        }
        departmentRepo.updateName(id, cmd.getNewName());
    }

    @Transactional
    public void deleteDept(Long actorId, Long id) {
        requireAdmin(actorId);
        Department dept = departmentRepo.findById(id);
        if (dept == null) {
            throw new ResourceNotFoundException("Department", id);
        }
        if (Boolean.TRUE.equals(dept.getIsDefault())) {
            throw new BusinessRuleViolationException("CANNOT_DELETE_DEFAULT_DEPT", "不能删除默认部门");
        }
        if ((dept.getTicketCount() != null && dept.getTicketCount() > 0)
                || (dept.getStaffCount() != null && dept.getStaffCount() > 0)) {
            throw new BusinessRuleViolationException("DEPT_NOT_EMPTY", "部门中还有工单或员工，不能删除");
        }
        departmentRepo.deleteById(id);
    }

    @Transactional
    public void migrateTickets(Long actorId, Long deptId, MigrateTicketsCmd cmd) {
        requireAdmin(actorId);
        if (departmentRepo.findById(deptId) == null) {
            throw new ResourceNotFoundException("Department", deptId);
        }
        if (deptId.equals(cmd.getTargetDepartmentId())) {
            throw new BusinessRuleViolationException("SAME_DEPARTMENT", "源部门与目标部门相同");
        }
        if (departmentRepo.findById(cmd.getTargetDepartmentId()) == null) {
            throw new ResourceNotFoundException("Department", cmd.getTargetDepartmentId());
        }
        departmentRepo.migrateTickets(deptId, cmd.getTargetDepartmentId());
    }

    @Transactional
    public void migrateStaff(Long actorId, Long deptId, MigrateStaffCmd cmd) {
        requireAdmin(actorId);
        if (departmentRepo.findById(deptId) == null) {
            throw new ResourceNotFoundException("Department", deptId);
        }
        if (deptId.equals(cmd.getTargetDepartmentId())) {
            throw new BusinessRuleViolationException("SAME_DEPARTMENT", "源部门与目标部门相同");
        }
        if (departmentRepo.findById(cmd.getTargetDepartmentId()) == null) {
            throw new ResourceNotFoundException("Department", cmd.getTargetDepartmentId());
        }
        departmentRepo.migrateStaff(deptId, cmd.getTargetDepartmentId());
    }

    private void requireAdmin(Long actorId) {
        Account account = actorId != null ? accountRepo.findById(actorId) : null;
        if (account == null || !account.isAdmin()) {
            throw new PermissionDeniedException("需要 L3 管理员权限");
        }
    }

    private DepartmentOverviewView toOverview(Department dept) {
        DepartmentOverviewView v = new DepartmentOverviewView();
        v.setId(dept.getId());
        v.setName(dept.getName());
        v.setTicketCount(dept.getTicketCount() != null ? dept.getTicketCount() : 0);
        v.setStaffCount(dept.getStaffCount() != null ? dept.getStaffCount() : 0);
        v.setDefault(Boolean.TRUE.equals(dept.getIsDefault()));
        v.setPrivate(Boolean.TRUE.equals(dept.getIsPrivate()));
        return v;
    }
}
