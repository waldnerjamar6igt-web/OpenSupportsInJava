package com.openSupports.demo.Domain.repo;

import com.openSupports.demo.Domain.Model.Account;
import com.openSupports.demo.Domain.Model.Staff;
import java.util.List;
import java.time.LocalDate;

public interface AccountRepo {
    Account findByEmail(String email);
    Account findById(Long id);
    int createAndSave(Account account);
    void update(Account account);
    void save(Account account);
    void changePassword(Long accountId, String hash, String salt);
    void changeEmail(String email, Long id, String excludeEmail);
    void setState(Long id, String state);
    void delete(Long id);
    List<Account> searchByEmailKeyword(String keyword, LocalDate from, LocalDate to, int offset, int size);
    int countSearchResults(String keyword, LocalDate from, LocalDate to);
    List<Account> findAll(int offset, int size);
    int countAll();
    Account createStaff(String email, String passwordHash, String salt, Integer level, Long deptId);
    void updateAccountLevel(Long id, Integer level);
    void updateAccountDept(Long id, Long deptId);
    void unAssignAllTickets(Long staffId);
    void closeAllTickets(Long staffId);
    void deleteByIdWithCascade(Long id);

    List<Staff> searchStaff(String keyword, Long departmentId, Integer level,
                            LocalDate from, LocalDate to, String sortBy, int offset, int size);
    int countStaff(String keyword, Long departmentId, Integer level, LocalDate from, LocalDate to);

    void unassignTicketsAuthoredBy(Long userId);
    void closeTicketsAuthoredBy(Long userId);
}
