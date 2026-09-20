package com.openSupports.demo.api.controller;

import com.openSupports.demo.api.dto.common.OperationResult;
import com.openSupports.demo.api.dto.common.PageResult;
import com.openSupports.demo.api.dto.staff.*;
import com.openSupports.demo.api.dto.user.*;
import com.openSupports.demo.application.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/auth/signup")
    public TokenResp signup(@Valid @RequestBody UserSignupCmd cmd) {
        return accountService.signup(cmd);
    }

    @PostMapping("/auth/signin")
    public TokenResp signin(@Valid @RequestBody UserSigninCmd cmd, HttpServletRequest request) {
        Map<String, Object> attrs = accountService.signin(cmd.getEmail(), cmd.getPassword(),
                request.getSession(), cmd.getRememberMe());
        return new TokenResp(
                (String) attrs.get("token"),
                attrs.get("userId").toString(),
                (Integer) attrs.get("staffLevel"),
                attrs.get("departmentId") != null ? attrs.get("departmentId").toString() : null,
                (Boolean) attrs.get("requireRememberCookie")
        );
    }

    @PutMapping("/account/email")
    public OperationResult changeEmail(@Valid @RequestBody ChangeEmailCmd cmd, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        accountService.changeEmail(userId, cmd.getNewEmail());
        return new OperationResult(true, "邮箱已更新");
    }

    @PutMapping("/account/password")
    public OperationResult changePassword(@Valid @RequestBody ChangePasswordCmd cmd, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        accountService.changePassword(userId, cmd.getCurrentPassword(), cmd.getNewPassword());
        return new OperationResult(true, "密码已更新");
    }

    // --- Admin endpoints ---

    @GetMapping("/admin/users")
    public PageResult<UserOverviewView> searchUsers(SearchUsersCmd cmd) {
        return accountService.searchUsers(cmd);
    }

    @GetMapping("/admin/users/{id}")
    public DetailedUserView getUserDetail(@PathVariable Long id) {
        return accountService.getUserDetail(id);
    }

    @PostMapping("/admin/staff")
    public OperationResult createStaff(@Valid @RequestBody CreateStaffCmd cmd) {
        accountService.createStaff(cmd);
        return new OperationResult(true, "员工账号已创建");
    }

    @PutMapping("/admin/staff/{id}/level")
    public OperationResult changeStaffLevel(@PathVariable Long id, @Valid @RequestBody ChangeStaffLevelCmd cmd,
                                            HttpServletRequest request) {
        accountService.changeStaffLevel((Long) request.getAttribute("currentUserId"), id, cmd.getLevel());
        return new OperationResult(true, "级别已更新");
    }

    @PutMapping("/admin/staff/{id}/department")
    public OperationResult changeStaffDept(@PathVariable Long id, @Valid @RequestBody ChangeStaffDeptCmd cmd,
                                           HttpServletRequest request) {
        accountService.changeStaffDept((Long) request.getAttribute("currentUserId"), id, cmd.getTargetDepartmentId());
        return new OperationResult(true, "部门已变更");
    }

    @PutMapping("/admin/staff/{id}/password")
    public OperationResult changeStaffPassword(@PathVariable Long id, @Valid @RequestBody ChangeStaffPasswordCmd cmd) {
        accountService.changeStaffPassword(id, cmd.getNewPassword());
        return new OperationResult(true, "密码已重置");
    }

    @PutMapping("/admin/staff/{id}/state")
    public OperationResult changeStaffState(@PathVariable Long id, @Valid @RequestBody ChangeStaffStateCmd cmd,
                                            HttpServletRequest request) {
        accountService.changeStaffState((Long) request.getAttribute("currentUserId"), id, cmd.getEnabled());
        return new OperationResult(true, "状态已更新");
    }

    @DeleteMapping("/admin/staff/{id}")
    public OperationResult deleteStaff(@PathVariable Long id, HttpServletRequest request) {
        accountService.deleteStaff((Long) request.getAttribute("currentUserId"), id);
        return new OperationResult(true, "员工账号已删除");
    }

    @GetMapping("/admin/staff")
    public PageResult<StaffOverviewView> searchStaff(SearchStaffCmd cmd) {
        return accountService.searchStaff(cmd);
    }

    @GetMapping("/admin/staff/{id}")
    public DetailedStaffView getStaffDetail(@PathVariable Long id) {
        return accountService.getStaffDetail(id);
    }

    @PostMapping("/admin/users")
    public OperationResult createUser(@Valid @RequestBody CreateUserCmd cmd) {
        accountService.createUser(cmd);
        return new OperationResult(true, "用户账号已创建");
    }

    @PutMapping("/admin/users/{id}/password")
    public OperationResult changeUserPassword(@PathVariable Long id, @Valid @RequestBody ChangeUserPasswordCmd cmd) {
        accountService.changeUserPassword(id, cmd.getNewPassword());
        return new OperationResult(true, "密码已重置");
    }

    @PutMapping("/admin/users/{id}/state")
    public OperationResult changeUserState(@PathVariable Long id, @Valid @RequestBody ChangeUserStateCmd cmd,
                                           HttpServletRequest request) {
        accountService.changeUserState((Long) request.getAttribute("currentUserId"), id, cmd.getEnabled());
        return new OperationResult(true, "状态已更新");
    }

    @DeleteMapping("/admin/users/{id}")
    public OperationResult deleteUser(@PathVariable Long id, HttpServletRequest request) {
        accountService.deleteUser((Long) request.getAttribute("currentUserId"), id);
        return new OperationResult(true, "用户账号已删除");
    }
}
