package com.openSupports.demo.api.controller;

import com.openSupports.demo.api.dto.common.OperationResult;
import com.openSupports.demo.api.dto.department.*;
import com.openSupports.demo.application.service.DepartmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public List<DepartmentOverviewView> listDepartments() {
        return departmentService.listDepartments();
    }

    @PostMapping
    public OperationResult createDept(@Valid @RequestBody CreateDeptCmd cmd, HttpServletRequest request) {
        departmentService.createDept((Long) request.getAttribute("currentUserId"), cmd);
        return new OperationResult(true, "部门已创建");
    }

    @PutMapping("/{id}")
    public OperationResult updateDeptName(@PathVariable Long id, @Valid @RequestBody UpdateDeptNameCmd cmd,
                                          HttpServletRequest request) {
        departmentService.updateDeptName((Long) request.getAttribute("currentUserId"), id, cmd);
        return new OperationResult(true, "部门名称已更新");
    }

    @DeleteMapping("/{id}")
    public OperationResult deleteDept(@PathVariable Long id, HttpServletRequest request) {
        departmentService.deleteDept((Long) request.getAttribute("currentUserId"), id);
        return new OperationResult(true, "部门已删除");
    }

    @PostMapping("/{id}/migrate-tickets")
    public OperationResult migrateTickets(@PathVariable Long id, @Valid @RequestBody MigrateTicketsCmd cmd,
                                          HttpServletRequest request) {
        departmentService.migrateTickets((Long) request.getAttribute("currentUserId"), id, cmd);
        return new OperationResult(true, "工单迁移完成");
    }

    @PutMapping("/{id}/migrate-staff")
    public OperationResult migrateStaff(@PathVariable Long id, @Valid @RequestBody MigrateStaffCmd cmd,
                                        HttpServletRequest request) {
        departmentService.migrateStaff((Long) request.getAttribute("currentUserId"), id, cmd);
        return new OperationResult(true, "员工迁移完成");
    }
}
