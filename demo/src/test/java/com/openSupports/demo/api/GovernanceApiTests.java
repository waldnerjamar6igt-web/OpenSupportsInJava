package com.openSupports.demo.api;

import com.openSupports.demo.ApiTestBase;
import com.openSupports.demo.api.dto.common.ApiError;
import com.openSupports.demo.api.dto.common.OperationResult;
import com.openSupports.demo.api.dto.common.PageResult;
import com.openSupports.demo.api.dto.staff.*;
import com.openSupports.demo.api.dto.ticket.TicketDetailView;
import com.openSupports.demo.api.dto.user.TokenResp;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** UC-12 ~ UC-15 用户 / 员工治理 */
class GovernanceApiTests extends ApiTestBase {

    private static final ParameterizedTypeReference<PageResult<UserOverviewView>> USER_PAGE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<PageResult<StaffOverviewView>> STAFF_PAGE = new ParameterizedTypeReference<>() {
    };

    private String newStaff(String prefix, int level, long deptId) {
        String email = prefix + "-" + uniqueSuffix() + "@test.local";
        ResponseEntity<String> resp = post("/api/v1/admin/staff",
                Map.of("email", email, "password", DEFAULT_PASSWORD, "level", level, "departmentId", deptId),
                adminCookie, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return email;
    }

    // ------------------------------------------------------------------
    // UC-12 用户搜索 & 治理界面
    // ------------------------------------------------------------------
    @Test
    void uc12_searchUsers_andDetailWithTickets() {
        ResponseEntity<PageResult<UserOverviewView>> all = getType("/api/v1/admin/users?page=1&pageSize=100",
                adminCookie, USER_PAGE);
        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(all.getBody().getContent()).extracting(UserOverviewView::getEmail)
                .contains(USER_EMAILS[0], USER_EMAILS[1]);

        ResponseEntity<PageResult<UserOverviewView>> byKeyword = getType(
                "/api/v1/admin/users?emailKeyword=user1&page=1&pageSize=100", adminCookie, USER_PAGE);
        assertThat(byKeyword.getBody().getContent()).extracting(UserOverviewView::getEmail)
                .allMatch(e -> e.contains("user1"));

        Long user1Id = userId(USER_EMAILS[0]);
        createTicket(cookie(USER_EMAILS[0]), deptAId, "UC12 ticket " + uniqueSuffix());

        ResponseEntity<DetailedUserView> detail = get("/api/v1/admin/users/" + user1Id, adminCookie, DetailedUserView.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().getEmail()).isEqualTo(USER_EMAILS[0]);
        assertThat(detail.getBody().getTickets()).isNotEmpty();
    }

    // ------------------------------------------------------------------
    // UC-13 Staff 高级搜索
    // ------------------------------------------------------------------
    @Test
    void uc13_searchStaff_andDetail() {
        ResponseEntity<PageResult<StaffOverviewView>> all = getType("/api/v1/admin/staff?page=1&pageSize=100",
                adminCookie, STAFF_PAGE);
        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(all.getBody().getContent()).extracting(StaffOverviewView::getEmail)
                .contains(L1_EMAILS[0], L2_EMAILS[0], L3_EMAILS[0]);

        ResponseEntity<PageResult<StaffOverviewView>> l1Only = getType(
                "/api/v1/admin/staff?level=1&page=1&pageSize=100", adminCookie, STAFF_PAGE);
        assertThat(l1Only.getBody().getContent()).isNotEmpty()
                .allMatch(s -> Integer.valueOf(1).equals(s.getLevel()));

        ResponseEntity<PageResult<StaffOverviewView>> deptA = getType(
                "/api/v1/admin/staff?departmentId=" + deptAId + "&page=1&pageSize=100", adminCookie, STAFF_PAGE);
        assertThat(deptA.getBody().getContent()).isNotEmpty()
                .allMatch(s -> deptAId.equals(s.getDepartmentId()));

        Long l1Id = userId(L1_EMAILS[0]);
        ResponseEntity<DetailedStaffView> detail = get("/api/v1/admin/staff/" + l1Id, adminCookie, DetailedStaffView.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().getLevel()).isEqualTo(1);
        assertThat(detail.getBody().getDepartmentName()).isNotBlank();
    }

    // ------------------------------------------------------------------
    // UC-14 员工管理
    // ------------------------------------------------------------------
    @Test
    void uc14_createStaff_login_level_dept_password_state_delete() {
        String email = newStaff("staff-crud", 1, deptAId);
        Long id = userId(email);

        // 可登录且级别正确
        assertThat(signinLevel(email, DEFAULT_PASSWORD)).isEqualTo(1);

        // 修改级别
        assertThat(put("/api/v1/admin/staff/" + id + "/level", Map.of("level", 2), adminCookie, OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signinLevel(email, DEFAULT_PASSWORD)).isEqualTo(2);

        // 变更部门
        assertThat(put("/api/v1/admin/staff/" + id + "/department",
                Map.of("targetDepartmentId", deptBId), adminCookie, OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/v1/admin/staff/" + id, adminCookie, DetailedStaffView.class)
                .getBody().getDepartmentId()).isEqualTo(deptBId);

        // 修改密码
        assertThat(put("/api/v1/admin/staff/" + id + "/password",
                Map.of("newPassword", "newpass123"), adminCookie, OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signinLevel(email, "newpass123")).isEqualTo(2);

        // 停用 -> 不可登录
        assertThat(put("/api/v1/admin/staff/" + id + "/state",
                Map.of("enabled", false), adminCookie, OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<ApiError> disabled = post("/api/v1/auth/signin",
                Map.of("email", email, "password", "newpass123"), null, ApiError.class);
        assertThat(disabled.getBody().getCode()).isEqualTo("ACCOUNT_DISABLED");

        // 启用
        assertThat(put("/api/v1/admin/staff/" + id + "/state",
                Map.of("enabled", true), adminCookie, OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signinLevel(email, "newpass123")).isEqualTo(2);

        // 删除
        assertThat(delete("/api/v1/admin/staff/" + id, adminCookie, OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/v1/admin/staff/" + id, adminCookie, ApiError.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void uc14_disableStaff_unassignsTickets() {
        String email = newStaff("staff-unassign", 1, deptAId);
        Long staffId = userId(email);
        TicketDetailView ticket = createTicket(cookie(USER_EMAILS[0]), deptAId, "UC14 unassign " + uniqueSuffix());
        put("/api/v1/tickets/" + ticket.getId() + "/assign", Map.of("staffId", staffId), adminCookie, OperationResult.class);

        put("/api/v1/admin/staff/" + staffId + "/state", Map.of("enabled", false), adminCookie, OperationResult.class);

        TicketDetailView reloaded = get("/api/v1/tickets/" + ticket.getId(), adminCookie, TicketDetailView.class).getBody();
        assertThat(reloaded.getAssigneeId()).isNull();
    }

    @Test
    void uc14_cannotDeleteOrDisableSelf() {
        Long adminId = userId(ADMIN_EMAIL);
        ResponseEntity<ApiError> deleteSelf = delete("/api/v1/admin/staff/" + adminId, adminCookie, ApiError.class);
        assertThat(deleteSelf.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(deleteSelf.getBody().getCode()).isEqualTo("CANNOT_DELETE_SELF");

        ResponseEntity<ApiError> disableSelf = put("/api/v1/admin/staff/" + adminId + "/state",
                Map.of("enabled", false), adminCookie, ApiError.class);
        assertThat(disableSelf.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(disableSelf.getBody().getCode()).isEqualTo("CANNOT_DISABLE_SELF");
    }

    // ------------------------------------------------------------------
    // UC-15 用户管理
    // ------------------------------------------------------------------
    @Test
    void uc15_createUser_password_state_delete_closesTickets() {
        String email = "user-crud-" + uniqueSuffix() + "@test.local";
        assertThat(post("/api/v1/admin/users", Map.of("email", email, "password", DEFAULT_PASSWORD),
                adminCookie, OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        String cookie = signin(email, DEFAULT_PASSWORD);
        assertThat(cookie).isNotBlank();
        Long id = userId(email);

        // 改密码
        assertThat(put("/api/v1/admin/users/" + id + "/password", Map.of("newPassword", "newpass123"),
                adminCookie, OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signin(email, "newpass123")).isNotBlank();

        // 建单后删除，工单应被关闭
        Login l = login(email, "newpass123");
        TicketDetailView ticket2 = createTicket(l.cookie, deptAId, "UC15 ticket " + uniqueSuffix());

        // 停用
        assertThat(put("/api/v1/admin/users/" + id + "/state", Map.of("enabled", false),
                adminCookie, OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(post("/api/v1/auth/signin", Map.of("email", email, "password", "newpass123"), null, ApiError.class)
                .getBody().getCode()).isEqualTo("ACCOUNT_DISABLED");

        // 删除
        assertThat(delete("/api/v1/admin/users/" + id, adminCookie, OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        TicketDetailView closed = get("/api/v1/tickets/" + ticket2.getId(), adminCookie, TicketDetailView.class).getBody();
        assertThat(closed.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    void uc15_cannotDeleteOrDisableSelf() {
        Long adminId = userId(ADMIN_EMAIL);
        ResponseEntity<ApiError> deleteSelf = delete("/api/v1/admin/users/" + adminId, adminCookie, ApiError.class);
        assertThat(deleteSelf.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(deleteSelf.getBody().getCode()).isEqualTo("CANNOT_DELETE_SELF");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private Integer signinLevel(String email, String password) {
        ResponseEntity<TokenResp> resp = post("/api/v1/auth/signin",
                Map.of("email", email, "password", password), null, TokenResp.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().getStaffLevel();
    }

    private record Login(String cookie, Long id) {
    }

    private Login login(String email, String password) {
        String cookie = signin(email, password);
        assertThat(cookie).isNotBlank();
        Long id = userMapper.findByEmail(email).getId();
        return new Login(cookie, id);
    }
}
