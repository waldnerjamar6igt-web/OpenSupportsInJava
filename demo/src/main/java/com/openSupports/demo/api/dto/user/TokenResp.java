package com.openSupports.demo.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResp {
    private String token;
    private String userId;
    private Integer staffLevel;
    private String departmentId;
    private Boolean requireRememberCookie;
}
