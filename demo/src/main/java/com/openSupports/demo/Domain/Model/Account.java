package com.openSupports.demo.Domain.Model;

import com.openSupports.demo.Domain.event.*;
import com.openSupports.demo.infra.exception.BusinessRuleViolationException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Account {
    private Long id;
    private String email;
    private String passwordHash;
    private String salt;
    private String state = "ENABLED";
    private LocalDateTime signUpTime;
    private LocalDateTime lastLoginTime;
    private String rememberToken;
    private LocalDateTime rememberTokenExpires;
    private String csrfUserid;
    private String csrfToken;
    private Integer level;
    private Long departmentId;
    private Integer ticketCount;
    private Set<Event> events = new HashSet<>();

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return this.email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return this.passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return this.salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getState() { return this.state; }
    public void setState(String state) { this.state = state; }
    public LocalDateTime getSignUpTime() { return this.signUpTime; }
    public void setSignUpTime(LocalDateTime signUpTime) { this.signUpTime = signUpTime; }
    public LocalDateTime getLastLoginTime() { return this.lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public String getRememberToken() { return this.rememberToken; }
    public void setRememberToken(String rememberToken) { this.rememberToken = rememberToken; }
    public LocalDateTime getRememberTokenExpires() { return this.rememberTokenExpires; }
    public void setRememberTokenExpires(LocalDateTime rememberTokenExpires) { this.rememberTokenExpires = rememberTokenExpires; }
    public String getCsrfUserid() { return this.csrfUserid; }
    public void setCsrfUserid(String csrfUserid) { this.csrfUserid = csrfUserid; }
    public String getCsrfToken() { return this.csrfToken; }
    public void setCsrfToken(String csrfToken) { this.csrfToken = csrfToken; }
    public Integer getLevel() { return this.level; }
    public void setLevel(Integer level) { this.level = level; }
    public Long getDepartmentId() { return this.departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Integer getTicketCount() { return this.ticketCount; }
    public void setTicketCount(Integer ticketCount) { this.ticketCount = ticketCount; }
    public Set<Event> getEvents() { return this.events; }
    public void setEvents(Set<Event> events) { this.events = events; }

    public boolean isEnabled() { return "ENABLED".equals(this.state); }
    public boolean isStaff() { return this.level != null; }
    public boolean isAdmin() { return Integer.valueOf(3).equals(this.level); }
    public boolean isSupervisor() { return Integer.valueOf(2).equals(this.level); }

    public void createUser(String email, String passwordHash, String salt) {
        if (this.email != null && !this.email.isEmpty()) {
            throw new BusinessRuleViolationException("ACCOUNT_ALREADY_EXISTS", "邮箱已被注册");
        }
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.state = "ENABLED";
        this.csrfUserid = UUID.randomUUID().toString().substring(0, 8);
        this.csrfToken = UUID.randomUUID().toString();
        this.signUpTime = LocalDateTime.now();
        events.add(new UserRegistered(email));
    }

    public void initAsStaff(Integer level, Long departmentId) {
        if (this.level != null) {
            throw new BusinessRuleViolationException("NOT_A_USER_ACCOUNT", "该账户已是员工账号");
        }
        this.level = level;
        this.departmentId = departmentId;
        events.add(new StaffCreated(level, departmentId));
    }

    public void changePassword(String newPasswordHash, String newSalt) {
        validateActive();
        this.passwordHash = newPasswordHash;
        this.salt = newSalt;
        events.add(new PasswordChanged());
    }

    public void editEmailAddress(String newEmail, String currentEmail) {
        validateActive();
        if (newEmail.equals(currentEmail)) {
            throw new BusinessRuleViolationException("EMAIL_SAME_AS_CURRENT", "新邮箱与当前邮箱相同");
        }
        this.email = newEmail;
        events.add(new EmailAddressChanged());
    }

    public void disable() {
        if ("DISABLED".equals(this.state)) {
            throw new BusinessRuleViolationException("ACCOUNT_ALREADY_DISABLED", "账户已被停用");
        }
        this.state = "DISABLED";
        events.add(new StaffDisabled());
    }

    public void enable() {
        if ("ENABLED".equals(this.state)) {
            throw new BusinessRuleViolationException("ALREADY_ENABLED", "账户已启用");
        }
        this.state = "ENABLED";
        events.add(new StaffEnabled());
    }

    public void deleteSelf() {
        events.add(new StaffDeleted());
    }

    public void updateRememberToken(String token, LocalDateTime expires) {
        this.rememberToken = token;
        this.rememberTokenExpires = expires;
    }

    public void recordLogin() {
        this.lastLoginTime = LocalDateTime.now();
    }

    private void validateActive() {
        if (!isEnabled()) {
            throw new BusinessRuleViolationException("ACCOUNT_DISABLED", "账户已被停用，无法执行此操作");
        }
    }

    public void clearEvents() { events.clear(); }
}
