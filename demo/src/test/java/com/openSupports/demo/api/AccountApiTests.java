package com.openSupports.demo.api;

import com.openSupports.demo.ApiTestBase;
import com.openSupports.demo.api.dto.common.ApiError;
import com.openSupports.demo.api.dto.common.OperationResult;
import com.openSupports.demo.api.dto.user.ChangeEmailCmd;
import com.openSupports.demo.api.dto.user.ChangePasswordCmd;
import com.openSupports.demo.api.dto.user.TokenResp;
import com.openSupports.demo.api.dto.user.UserSignupCmd;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** UC-01 注册 / UC-02 登录 / UC-03 个人资料管理 */
class AccountApiTests extends ApiTestBase {

    private String uniqueEmail() {
        return "acc-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
    }

    @Test
    void uc01_signup_success_returnsTokenAndUserId() {
        String email = uniqueEmail();
        UserSignupCmd cmd = new UserSignupCmd();
        cmd.setEmail(email);
        cmd.setPassword("secret123");

        ResponseEntity<TokenResp> resp = post("/api/v1/auth/signup", cmd, null, TokenResp.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getUserId()).isNotBlank();
        assertThat(resp.getBody().getStaffLevel()).isNull();
    }

    @Test
    void uc01_signup_duplicateEmail_rejected() {
        String email = uniqueEmail();
        UserSignupCmd cmd = new UserSignupCmd();
        cmd.setEmail(email);
        cmd.setPassword("secret123");
        post("/api/v1/auth/signup", cmd, null, TokenResp.class);

        ResponseEntity<ApiError> resp = post("/api/v1/auth/signup", cmd, null, ApiError.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    void uc01_signup_shortPassword_rejected() {
        UserSignupCmd cmd = new UserSignupCmd();
        cmd.setEmail(uniqueEmail());
        cmd.setPassword("123");
        ResponseEntity<ApiError> resp = post("/api/v1/auth/signup", cmd, null, ApiError.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void uc02_signin_admin_returnsLevel3() {
        ResponseEntity<TokenResp> resp = post("/api/v1/auth/signin",
                Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD), null, TokenResp.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getStaffLevel()).isEqualTo(3);
        assertThat(extractSessionCookie(resp.getHeaders())).isNotBlank();
    }

    @Test
    void uc02_signin_staffLevelsCorrect() {
        assertThat(signinLevel(L1_EMAILS[0])).isEqualTo(1);
        assertThat(signinLevel(L2_EMAILS[0])).isEqualTo(2);
        assertThat(signinLevel(L3_EMAILS[0])).isEqualTo(3);
    }

    private Integer signinLevel(String email) {
        ResponseEntity<TokenResp> resp = post("/api/v1/auth/signin",
                Map.of("email", email, "password", DEFAULT_PASSWORD), null, TokenResp.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().getStaffLevel();
    }

    @Test
    void uc02_signin_wrongPassword_rejected() {
        ResponseEntity<ApiError> resp = post("/api/v1/auth/signin",
                Map.of("email", ADMIN_EMAIL, "password", "wrong-password"), null, ApiError.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCode()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void uc02_signin_disabledAccount_rejected() {
        // 创建一个用户并由管理员停用
        String email = uniqueEmail();
        signupUser(email);
        Long id = userId(email);
        put("/api/v1/admin/users/" + id + "/state", Map.of("enabled", false), adminCookie, String.class);

        ResponseEntity<ApiError> resp = post("/api/v1/auth/signin",
                Map.of("email", email, "password", DEFAULT_PASSWORD), null, ApiError.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCode()).isEqualTo("ACCOUNT_DISABLED");
    }

    @Test
    void uc03_changeEmail_successAndConflict() {
        String email = uniqueEmail();
        signupUser(email);
        String cookie = signin(email, DEFAULT_PASSWORD);
        assertThat(cookie).isNotBlank();

        String newEmail = uniqueEmail();
        ChangeEmailCmd cmd = new ChangeEmailCmd();
        cmd.setNewEmail(newEmail);
        ResponseEntity<OperationResult> resp = put("/api/v1/account/email", cmd, cookie, OperationResult.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 新邮箱可登录
        assertThat(signin(newEmail, DEFAULT_PASSWORD)).isNotBlank();

        // 冲突：与已有用户邮箱相同
        ChangeEmailCmd conflict = new ChangeEmailCmd();
        conflict.setNewEmail(USER_EMAILS[1]);
        ResponseEntity<ApiError> err = put("/api/v1/account/email", conflict,
                cookieByEmail.get(USER_EMAILS[0]), ApiError.class);
        assertThat(err.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(err.getBody().getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    void uc03_changePassword_successAndWrongCurrent() {
        String email = uniqueEmail();
        signupUser(email);
        String cookie = signin(email, DEFAULT_PASSWORD);

        ChangePasswordCmd wrong = new ChangePasswordCmd();
        wrong.setCurrentPassword("nope-wrong");
        wrong.setNewPassword("newpass123");
        ResponseEntity<ApiError> err = put("/api/v1/account/password", wrong, cookie, ApiError.class);
        assertThat(err.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(err.getBody().getCode()).isEqualTo("WRONG_PASSWORD");

        ChangePasswordCmd ok = new ChangePasswordCmd();
        ok.setCurrentPassword(DEFAULT_PASSWORD);
        ok.setNewPassword("newpass123");
        ResponseEntity<OperationResult> resp = put("/api/v1/account/password", ok, cookie, OperationResult.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signin(email, "newpass123")).isNotBlank();
        assertThat(signin(email, DEFAULT_PASSWORD)).isNull();
    }

    @Test
    void unauthenticatedProtectedEndpoint_rejected() {
        ResponseEntity<ApiError> resp = get("/api/v1/admin/users", null, ApiError.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void signupUser(String email) {
        UserSignupCmd cmd = new UserSignupCmd();
        cmd.setEmail(email);
        cmd.setPassword(DEFAULT_PASSWORD);
        post("/api/v1/auth/signup", cmd, null, TokenResp.class);
    }
}
