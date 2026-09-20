package com.openSupports.demo.Domain.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String email;
    private String passwordHash;
    private String salt;
    private String state;
    private LocalDateTime signUpTime;
    private LocalDateTime lastLoginTime;
    private String rememberToken;
    private LocalDateTime rememberTokenExpires;
    private String csrfUserid;
    private String csrfToken;
    private Integer ticketCount;
}
