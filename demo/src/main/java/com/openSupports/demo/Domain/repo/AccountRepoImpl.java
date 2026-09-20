package com.openSupports.demo.Domain.repo;

import com.openSupports.demo.Domain.Model.Account;
import com.openSupports.demo.Domain.Model.Staff;
import com.openSupports.demo.Domain.Model.User;
import com.openSupports.demo.infra.mapper.StafaMapper;
import com.openSupports.demo.infra.mapper.TicketMapper;
import com.openSupports.demo.infra.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountRepoImpl implements AccountRepo {

    private final UserMapper userMapper;
    private final StafaMapper stafaMapper;
    private final TicketMapper ticketMapper;

    public AccountRepoImpl(UserMapper userMapper, StafaMapper stafaMapper, TicketMapper ticketMapper) {
        this.userMapper = userMapper;
        this.stafaMapper = stafaMapper;
        this.ticketMapper = ticketMapper;
    }

    @Override
    public Account findByEmail(String email) {
        Staff staff = stafaMapper.findByEmail(email);
        if (staff != null) {
            return mapStaffToAccount(staff);
        }
        User user = userMapper.findByEmail(email);
        if (user != null) {
            return mapUserToAccount(user);
        }
        return null;
    }

    @Override
    public Account findById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            return null;
        }
        Staff staff = stafaMapper.findByUserId(id);
        if (staff != null) {
            return mergeStaffIntoAccount(mapUserToAccount(user), staff);
        }
        return mapUserToAccount(user);
    }

    @Override
    public int createAndSave(Account account) {
        User user = toUser(account);
        if (user.getCsrfUserid() == null) {
            user.setCsrfUserid("");
        }
        if (user.getCsrfToken() == null) {
            user.setCsrfToken("");
        }
        int rows = userMapper.insert(user);
        account.setId(user.getId());
        return rows;
    }

    @Override
    public void update(Account account) {
        User currentUser = userMapper.findById(account.getId());
        if (currentUser == null) {
            throw new IllegalArgumentException("Account not found: " + account.getId());
        }
        if (!account.getEmail().equals(currentUser.getEmail())) {
            userMapper.changeEmail(account.getId(), account.getEmail(), account.getEmail());
        }
        if (account.getPasswordHash() != null && !account.getPasswordHash().equals(currentUser.getPasswordHash())) {
            userMapper.updatePassword(toUser(account));
        }
    }

    @Override
    public void save(Account account) {
        update(account);
    }

    @Override
    public void changePassword(Long accountId, String hash, String salt) {
        User tmp = new User();
        tmp.setId(accountId);
        tmp.setPasswordHash(hash);
        tmp.setSalt(salt);
        userMapper.updatePassword(tmp);
    }

    @Override
    public void changeEmail(String email, Long id, String excludeEmail) {
        userMapper.changeEmail(id, email, excludeEmail);
    }

    @Override
    public void setState(Long id, String state) {
        userMapper.setState(id, state);
        if (stafaMapper.findByUserId(id) != null) {
            stafaMapper.setState(id, state);
        }
    }

    @Override
    public void delete(Long id) {
        if (stafaMapper.findByUserId(id) != null) {
            stafaMapper.deleteById(id);
        }
        userMapper.deleteById(id);
    }

    @Override
    public List<Account> searchByEmailKeyword(String keyword, LocalDate from, LocalDate to, int offset, int size) {
        String dateFrom = from != null ? from.toString() : null;
        String dateTo = to != null ? to.toString() : null;
        List<Account> results = new ArrayList<>();
        for (User u : userMapper.searchByKeyword(keyword, dateFrom, dateTo, "sign_up_time", size, offset)) {
            results.add(mapUserToAccount(u));
        }
        return results;
    }

    @Override
    public int countSearchResults(String keyword, LocalDate from, LocalDate to) {
        String dateFrom = from != null ? from.toString() : null;
        String dateTo = to != null ? to.toString() : null;
        return userMapper.countSearchResults(keyword, dateFrom, dateTo);
    }

    @Override
    public List<Account> findAll(int offset, int size) {
        List<Account> results = new ArrayList<>();
        for (User u : userMapper.findAll(size, offset)) {
            results.add(mapUserToAccount(u));
        }
        return results;
    }

    @Override
    public int countAll() {
        return userMapper.countAll();
    }

    @Override
    public Account createStaff(String email, String passwordHash, String salt, Integer level, Long deptId) {
        User user = toUser(new Account());
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setSalt(salt);
        user.setState("ENABLED");
        user.setCsrfUserid("");
        user.setCsrfToken("");
        userMapper.insert(user);
        Long userId = user.getId();

        Staff staff = new Staff();
        staff.setId(userId);
        staff.setLevel(level);
        staff.setDepartmentId(deptId);
        staff.setState("ENABLED");
        stafaMapper.insertStaff(staff);

        return findById(userId);
    }

    @Override
    public void updateAccountLevel(Long accountId, Integer newLevel) {
        stafaMapper.updateLevel(accountId, newLevel);
    }

    @Override
    public void updateAccountDept(Long accountId, Long newDeptId) {
        stafaMapper.changeDepartment(accountId, newDeptId);
    }

    @Override
    public void unAssignAllTickets(Long staffId) {
        stafaMapper.unAssignAllTickets(staffId);
    }

    @Override
    public void closeAllTickets(Long staffId) {
        stafaMapper.closeAllTickets(staffId);
    }

    @Override
    public void deleteByIdWithCascade(Long id) {
        stafaMapper.deleteById(id);
    }

    @Override
    public List<Staff> searchStaff(String keyword, Long departmentId, Integer level,
                                   LocalDate from, LocalDate to, String sortBy, int offset, int size) {
        String orderColumn = sortBy != null ? sortBy : "sign_up_time";
        if ("sign_up_time".equals(orderColumn)) {
            orderColumn = "u.sign_up_time";
        }
        return stafaMapper.searchWithFilters(keyword, departmentId, level,
                from != null ? from.toString() : null, to != null ? to.toString() : null,
                orderColumn, size, offset);
    }

    @Override
    public int countStaff(String keyword, Long departmentId, Integer level, LocalDate from, LocalDate to) {
        return stafaMapper.countSearchResults(keyword, departmentId, level,
                from != null ? from.toString() : null, to != null ? to.toString() : null);
    }

    @Override
    public void unassignTicketsAuthoredBy(Long userId) {
        ticketMapper.unAssignTicketsByAuthor(userId);
    }

    @Override
    public void closeTicketsAuthoredBy(Long userId) {
        ticketMapper.closeTicketsByAuthor(userId);
    }

    // ===== Internal helper methods =====

    private Account mapStaffToAccount(Staff staff) {
        Account acc = new Account();
        acc.setId(staff.getId());
        acc.setEmail(staff.getEmail());
        acc.setPasswordHash(staff.getPasswordHash());
        acc.setSalt(staff.getSalt());
        acc.setState(staff.getState());
        acc.setSignUpTime(staff.getSignUpTime());
        acc.setLastLoginTime(staff.getLastLoginTime());
        acc.setRememberToken(staff.getRememberToken());
        acc.setRememberTokenExpires(staff.getRememberTokenExpires());
        acc.setCsrfUserid(staff.getCsrfUserid());
        acc.setCsrfToken(staff.getCsrfToken());
        acc.setLevel(staff.getLevel());
        acc.setDepartmentId(staff.getDepartmentId());
        acc.setTicketCount(staff.getTicketAssignedCount());
        return acc;
    }

    private Account mapUserToAccount(User user) {
        Account acc = new Account();
        acc.setId(user.getId());
        acc.setEmail(user.getEmail());
        acc.setPasswordHash(user.getPasswordHash());
        acc.setSalt(user.getSalt());
        acc.setState(user.getState());
        acc.setSignUpTime(user.getSignUpTime());
        acc.setLastLoginTime(user.getLastLoginTime());
        acc.setRememberToken(user.getRememberToken());
        acc.setRememberTokenExpires(user.getRememberTokenExpires());
        acc.setCsrfUserid(user.getCsrfUserid());
        acc.setCsrfToken(user.getCsrfToken());
        acc.setLevel(null);
        acc.setDepartmentId(null);
        acc.setTicketCount(user.getTicketCount());
        return acc;
    }

    private Account mergeStaffIntoAccount(Account base, Staff staff) {
        base.setLevel(staff.getLevel());
        base.setDepartmentId(staff.getDepartmentId());
        return base;
    }

    private User toUser(Account acc) {
        User u = new User();
        u.setId(acc.getId());
        u.setEmail(acc.getEmail());
        u.setPasswordHash(acc.getPasswordHash());
        u.setSalt(acc.getSalt());
        u.setState(acc.getState());
        u.setSignUpTime(acc.getSignUpTime());
        u.setLastLoginTime(acc.getLastLoginTime());
        u.setRememberToken(acc.getRememberToken());
        u.setRememberTokenExpires(acc.getRememberTokenExpires());
        u.setCsrfUserid(acc.getCsrfUserid());
        u.setCsrfToken(acc.getCsrfToken());
        return u;
    }
}
