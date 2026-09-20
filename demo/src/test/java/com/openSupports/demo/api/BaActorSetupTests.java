package com.openSupports.demo.api;

import com.openSupports.demo.ApiTestBase;
import com.openSupports.demo.api.dto.user.TokenResp;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验收基线：每个测试运行都保证存在 2 个 user 以及 L1/L2/L3 各 2 个 staff，且均可通过 API 登录。
 */
class BaActorSetupTests extends ApiTestBase {

    @Test
    void twoUsersExistAndCanSignIn() {
        assertThat(USER_EMAILS).hasSize(2);
        for (String email : USER_EMAILS) {
            assertThat(userMapper.findByEmail(email)).as("user exists: %s", email).isNotNull();
            ResponseEntity<TokenResp> resp = post("/api/v1/auth/signin",
                    Map.of("email", email, "password", DEFAULT_PASSWORD), null, TokenResp.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody().getStaffLevel()).isNull();
        }
    }

    @Test
    void twoStaffPerLevelExistAndCanSignIn() {
        assertThat(L1_EMAILS).hasSize(2);
        assertThat(L2_EMAILS).hasSize(2);
        assertThat(L3_EMAILS).hasSize(2);

        assertLevel(L1_EMAILS, 1);
        assertLevel(L2_EMAILS, 2);
        assertLevel(L3_EMAILS, 3);
    }

    private void assertLevel(String[] emails, int expectedLevel) {
        for (String email : emails) {
            assertThat(stafaMapper.findByEmail(email)).as("staff exists: %s", email).isNotNull();
            assertThat(stafaMapper.findByEmail(email).getLevel()).isEqualTo(expectedLevel);
            ResponseEntity<TokenResp> resp = post("/api/v1/auth/signin",
                    Map.of("email", email, "password", DEFAULT_PASSWORD), null, TokenResp.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody().getStaffLevel()).isEqualTo(expectedLevel);
        }
    }
}
