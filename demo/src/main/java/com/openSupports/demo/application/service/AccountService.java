package com.openSupports.demo.application.service;

import com.openSupports.demo.Domain.Model.Account;
import com.openSupports.demo.Domain.Model.Staff;
import com.openSupports.demo.Domain.Model.Ticket;
import com.openSupports.demo.Domain.repo.AccountRepo;
import com.openSupports.demo.Domain.repo.DepartmentRepo;
import com.openSupports.demo.Domain.repo.TicketRepo;
import com.openSupports.demo.api.dto.common.PageResult;
import com.openSupports.demo.api.dto.staff.*;
import com.openSupports.demo.api.dto.ticket.TicketListItemView;
import com.openSupports.demo.api.dto.user.*;
import com.openSupports.demo.infra.exception.BusinessRuleViolationException;
import com.openSupports.demo.infra.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepo accountRepo;
    private final DepartmentRepo departmentRepo;
    private final TicketRepo ticketRepo;

    // ------------------------------------------------------------------
    // UC-01 注册
    // ------------------------------------------------------------------
    @Transactional
    public TokenResp signup(UserSignupCmd cmd) {
        if (accountRepo.findByEmail(cmd.getEmail()) != null) {
            throw new BusinessRuleViolationException("EMAIL_ALREADY_EXISTS", "邮箱已被注册");
        }
        String salt = UUID.randomUUID().toString();
        String hash = BCrypt.hashpw(cmd.getPassword() + salt, BCrypt.gensalt());

        Account account = new Account();
        account.createUser(cmd.getEmail(), hash, salt);
        accountRepo.createAndSave(account);
        account.clearEvents();
        return buildTokenResponse(account);
    }

    // ------------------------------------------------------------------
    // UC-02 登录
    // ------------------------------------------------------------------
    @Transactional
    public Map<String, Object> signin(String email, String password, HttpSession session, Boolean rememberMe) {
        Account user = accountRepo.findByEmail(email);
        if (user == null || !BCrypt.checkpw(password + user.getSalt(), user.getPasswordHash())) {
            throw new BusinessRuleViolationException("INVALID_CREDENTIALS", "邮箱或密码错误");
        }
        if (!user.isEnabled()) {
            throw new BusinessRuleViolationException("ACCOUNT_DISABLED", "账户已被停用");
        }
        user.recordLogin();
        accountRepo.save(user);

        session.setAttribute("userId", user.getId());
        session.setAttribute("userRole", user.isStaff() ? "STAFF_" + user.getLevel() : "USER");
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("departmentId", user.getDepartmentId());

        boolean remember = Boolean.TRUE.equals(rememberMe);
        if (remember) {
            session.setMaxInactiveInterval(30 * 24 * 60 * 60);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", session.getId());
        result.put("userId", user.getId());
        result.put("staffLevel", user.getLevel());
        result.put("departmentId", user.getDepartmentId());
        result.put("requireRememberCookie", remember);
        return result;
    }

    // ------------------------------------------------------------------
    // UC-03 个人资料
    // ------------------------------------------------------------------
    @Transactional
    public void changeEmail(Long userId, String newEmail) {
        Account user = accountRepo.findById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User", userId);
        }
        Account existing = accountRepo.findByEmail(newEmail);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BusinessRuleViolationException("EMAIL_ALREADY_EXISTS", "邮箱已被他人使用");
        }
        user.editEmailAddress(newEmail, user.getEmail());
        accountRepo.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        Account user = accountRepo.findById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User", userId);
        }
        if (!BCrypt.checkpw(currentPassword + user.getSalt(), user.getPasswordHash())) {
            throw new BusinessRuleViolationException("WRONG_PASSWORD", "当前密码不正确");
        }
        applyPassword(userId, newPassword);
    }

    // ------------------------------------------------------------------
    // UC-12 用户搜索与治理界面
    // ------------------------------------------------------------------
    public PageResult<UserOverviewView> searchUsers(SearchUsersCmd cmd) {
        int page = cmd.getPage() != null ? cmd.getPage() : 1;
        int size = cmd.getPageSize() != null ? cmd.getPageSize() : 10;
        int offset = (page - 1) * size;

        List<Account> accounts;
        int total;
        if (cmd.getEmailKeyword() != null && !cmd.getEmailKeyword().isEmpty()) {
            accounts = accountRepo.searchByEmailKeyword(cmd.getEmailKeyword(),
                    cmd.getRegisterDateFrom(), cmd.getRegisterDateTo(), offset, size);
            total = accountRepo.countSearchResults(cmd.getEmailKeyword(),
                    cmd.getRegisterDateFrom(), cmd.getRegisterDateTo());
        } else {
            accounts = accountRepo.findAll(offset, size);
            total = accountRepo.countAll();
        }

        List<UserOverviewView> views = accounts.stream().map(a -> {
            UserOverviewView v = new UserOverviewView();
            v.setId(a.getId());
            v.setEmail(a.getEmail());
            v.setSignUpTime(a.getSignUpTime());
            v.setTicketCount(a.getTicketCount() != null ? a.getTicketCount() : 0);
            v.setState(a.getState());
            return v;
        }).toList();
        return PageResult.of(views, total, page, size);
    }

    public DetailedUserView getUserDetail(Long id) {
        Account user = accountRepo.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }
        DetailedUserView view = new DetailedUserView();
        view.setId(user.getId());
        view.setEmail(user.getEmail());
        view.setSignUpTime(user.getSignUpTime());
        view.setState(user.getState());
        view.setTickets(toListItems(ticketRepo.searchMeSent(id, 0, 200)));
        return view;
    }

    // ------------------------------------------------------------------
    // UC-13 Staff 高级搜索
    // ------------------------------------------------------------------
    public PageResult<StaffOverviewView> searchStaff(SearchStaffCmd cmd) {
        int page = cmd.getPage() != null ? cmd.getPage() : 1;
        int size = cmd.getPageSize() != null ? cmd.getPageSize() : 10;
        int offset = (page - 1) * size;

        List<Staff> staffList = accountRepo.searchStaff(cmd.getEmailKeyword(), cmd.getDepartmentId(),
                cmd.getLevel(), cmd.getRegisterDateFrom(), cmd.getRegisterDateTo(),
                cmd.getSortBy(), offset, size);
        int total = accountRepo.countStaff(cmd.getEmailKeyword(), cmd.getDepartmentId(),
                cmd.getLevel(), cmd.getRegisterDateFrom(), cmd.getRegisterDateTo());

        List<StaffOverviewView> views = staffList.stream().map(this::toStaffOverview).toList();
        return PageResult.of(views, total, page, size);
    }

    public DetailedStaffView getStaffDetail(Long id) {
        Account account = accountRepo.findById(id);
        if (account == null || !account.isStaff()) {
            throw new ResourceNotFoundException("Staff", id);
        }
        DetailedStaffView view = new DetailedStaffView();
        view.setId(account.getId());
        view.setEmail(account.getEmail());
        view.setLevel(account.getLevel());
        view.setDepartmentId(account.getDepartmentId());
        view.setDepartmentName(departmentName(account.getDepartmentId()));
        view.setTicketAssignedCount(account.getTicketCount() != null ? account.getTicketCount() : 0);
        view.setState(account.getState());
        view.setSignUpTime(account.getSignUpTime());
        view.setTicketsAssigned(toListItems(ticketRepo.searchMyAssigned(id, 0, 200)));
        return view;
    }

    // ------------------------------------------------------------------
    // UC-14 Staff 管理
    // ------------------------------------------------------------------
    @Transactional
    public void createStaff(CreateStaffCmd cmd) {
        if (accountRepo.findByEmail(cmd.getEmail()) != null) {
            throw new BusinessRuleViolationException("EMAIL_ALREADY_EXISTS", "邮箱已被注册");
        }
        if (departmentRepo.findById(cmd.getDepartmentId()) == null) {
            throw new ResourceNotFoundException("Department", cmd.getDepartmentId());
        }
        String salt = UUID.randomUUID().toString();
        String hash = BCrypt.hashpw(cmd.getPassword() + salt, BCrypt.gensalt());
        accountRepo.createStaff(cmd.getEmail(), hash, salt, cmd.getLevel(), cmd.getDepartmentId());
    }

    @Transactional
    public void changeStaffLevel(Long actorId, Long staffId, Integer level) {
        requireStaff(staffId);
        if (level == null || level < 1 || level > 3) {
            throw new BusinessRuleViolationException("INVALID_LEVEL", "级别必须在1-3之间");
        }
        accountRepo.updateAccountLevel(staffId, level);
    }

    @Transactional
    public void changeStaffDept(Long actorId, Long staffId, Long targetDeptId) {
        requireStaff(staffId);
        if (departmentRepo.findById(targetDeptId) == null) {
            throw new ResourceNotFoundException("Department", targetDeptId);
        }
        // 变更部门时解除其全部工单分配（UC-14）
        accountRepo.unAssignAllTickets(staffId);
        accountRepo.updateAccountDept(staffId, targetDeptId);
    }

    @Transactional
    public void changeStaffPassword(Long staffId, String newPassword) {
        requireStaff(staffId);
        applyPassword(staffId, newPassword);
    }

    @Transactional
    public void changeStaffState(Long actorId, Long staffId, Boolean enabled) {
        requireStaff(staffId);
        if (Boolean.FALSE.equals(enabled) && staffId.equals(actorId)) {
            throw new BusinessRuleViolationException("CANNOT_DISABLE_SELF", "不能停用自己");
        }
        if (Boolean.FALSE.equals(enabled)) {
            accountRepo.unAssignAllTickets(staffId);
        }
        accountRepo.setState(staffId, Boolean.TRUE.equals(enabled) ? "ENABLED" : "DISABLED");
    }

    @Transactional
    public void deleteStaff(Long actorId, Long staffId) {
        requireStaff(staffId);
        if (staffId.equals(actorId)) {
            throw new BusinessRuleViolationException("CANNOT_DELETE_SELF", "不能删除自己");
        }
        accountRepo.unAssignAllTickets(staffId);
        accountRepo.closeAllTickets(staffId);
        accountRepo.delete(staffId);
    }

    // ------------------------------------------------------------------
    // UC-15 User 管理
    // ------------------------------------------------------------------
    @Transactional
    public void createUser(CreateUserCmd cmd) {
        if (accountRepo.findByEmail(cmd.getEmail()) != null) {
            throw new BusinessRuleViolationException("EMAIL_ALREADY_EXISTS", "邮箱已被注册");
        }
        String salt = UUID.randomUUID().toString();
        String hash = BCrypt.hashpw(cmd.getPassword() + salt, BCrypt.gensalt());
        Account account = new Account();
        account.createUser(cmd.getEmail(), hash, salt);
        accountRepo.createAndSave(account);
    }

    @Transactional
    public void changeUserPassword(Long userId, String newPassword) {
        requireUser(userId);
        applyPassword(userId, newPassword);
    }

    @Transactional
    public void changeUserState(Long actorId, Long userId, Boolean enabled) {
        requireUser(userId);
        if (Boolean.FALSE.equals(enabled) && userId.equals(actorId)) {
            throw new BusinessRuleViolationException("CANNOT_DISABLE_SELF", "不能停用自己");
        }
        accountRepo.setState(userId, Boolean.TRUE.equals(enabled) ? "ENABLED" : "DISABLED");
    }

    @Transactional
    public void deleteUser(Long actorId, Long userId) {
        requireUser(userId);
        if (userId.equals(actorId)) {
            throw new BusinessRuleViolationException("CANNOT_DELETE_SELF", "不能删除自己");
        }
        // 其发出的工单全部解除分配并关闭（UC-15）
        accountRepo.unassignTicketsAuthoredBy(userId);
        accountRepo.closeTicketsAuthoredBy(userId);
        accountRepo.delete(userId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private void applyPassword(Long accountId, String newPassword) {
        String salt = UUID.randomUUID().toString();
        String hash = BCrypt.hashpw(newPassword + salt, BCrypt.gensalt());
        accountRepo.changePassword(accountId, hash, salt);
    }

    private void requireStaff(Long staffId) {
        Account account = accountRepo.findById(staffId);
        if (account == null || !account.isStaff()) {
            throw new ResourceNotFoundException("Staff", staffId);
        }
    }

    private void requireUser(Long userId) {
        if (accountRepo.findById(userId) == null) {
            throw new ResourceNotFoundException("User", userId);
        }
    }

    private String departmentName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        var dept = departmentRepo.findById(deptId);
        return dept != null ? dept.getName() : null;
    }

    private StaffOverviewView toStaffOverview(Staff staff) {
        StaffOverviewView v = new StaffOverviewView();
        v.setId(staff.getId());
        v.setEmail(staff.getEmail());
        v.setLevel(staff.getLevel());
        v.setDepartmentId(staff.getDepartmentId());
        v.setDepartmentName(staff.getDepartmentName());
        v.setTicketAssignedCount(staff.getTicketAssignedCount() != null ? staff.getTicketAssignedCount() : 0);
        v.setState(staff.getState());
        v.setSignUpTime(staff.getSignUpTime());
        return v;
    }

    private List<TicketListItemView> toListItems(List<Ticket> tickets) {
        List<TicketListItemView> views = new ArrayList<>();
        if (tickets == null) {
            return views;
        }
        for (Ticket t : tickets) {
            TicketListItemView v = new TicketListItemView();
            v.setId(t.getId());
            v.setTicketCode(t.getTicketCode());
            v.setTitle(t.getTitle());
            v.setStatus(t.getStatus());
            v.setPriority(t.getPriority());
            v.setDepartmentName(t.getDepartmentName());
            v.setAssigneeName(t.getAssigneeName());
            v.setAuthorId(t.getAuthorId());
            v.setAuthorEmail(t.getAuthorEmail());
            v.setCreatedAt(t.getCreatedAt());
            v.setLastActivityAt(t.getLastActivityAt());
            v.setEditedTitle(Boolean.TRUE.equals(t.getEditedTitle()));
            v.setUnreadCount(t.getUnreadCount() != null ? t.getUnreadCount() : 0);
            views.add(v);
        }
        return views;
    }

    private TokenResp buildTokenResponse(Account account) {
        return new TokenResp(
                UUID.randomUUID().toString(),
                account.getId() != null ? account.getId().toString() : null,
                account.getLevel(),
                account.getDepartmentId() != null ? account.getDepartmentId().toString() : null,
                false
        );
    }
}
