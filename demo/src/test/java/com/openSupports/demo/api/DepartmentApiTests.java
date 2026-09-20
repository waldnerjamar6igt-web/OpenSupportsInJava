package com.openSupports.demo.api;

import com.openSupports.demo.ApiTestBase;
import com.openSupports.demo.api.dto.common.ApiError;
import com.openSupports.demo.api.dto.common.OperationResult;
import com.openSupports.demo.api.dto.department.DepartmentOverviewView;
import com.openSupports.demo.api.dto.staff.DetailedStaffView;
import com.openSupports.demo.api.dto.ticket.TicketDetailView;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** UC-18 部门管理 */
class DepartmentApiTests extends ApiTestBase {

    private Long createDepartment(String name) {
        ResponseEntity<OperationResult> resp = post("/api/v1/departments", Map.of("name", name),
                adminCookie, OperationResult.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return departmentId(name);
    }

    private Long departmentId(String name) {
        List<DepartmentOverviewView> depts = Arrays.asList(
                get("/api/v1/departments", adminCookie, DepartmentOverviewView[].class).getBody());
        return depts.stream().filter(d -> name.equals(d.getName())).findFirst().orElseThrow().getId();
    }

    private String createStaff(String email, int level, long deptId) {
        assertThat(post("/api/v1/admin/staff",
                Map.of("email", email, "password", DEFAULT_PASSWORD, "level", level, "departmentId", deptId),
                adminCookie, OperationResult.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        return email;
    }

    @Test
    void uc18_listDepartments_includesDefault() {
        ResponseEntity<DepartmentOverviewView[]> resp = get("/api/v1/departments", adminCookie, DepartmentOverviewView[].class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Arrays.stream(resp.getBody()).map(DepartmentOverviewView::getName)).contains("Software Support");
        assertThat(Arrays.stream(resp.getBody()).anyMatch(d -> Boolean.TRUE.equals(d.getIsDefault()))).isTrue();
    }

    @Test
    void uc18_createDuplicateRenameDelete() {
        String name = "Dept-" + uniqueSuffix();
        createDepartment(name);

        // 重名
        ResponseEntity<ApiError> dup = post("/api/v1/departments", Map.of("name", name), adminCookie, ApiError.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(dup.getBody().getCode()).isEqualTo("DEPT_NAME_DUPLICATE");

        Long id = departmentId(name);
        String renamed = name + "-renamed";
        assertThat(put("/api/v1/departments/" + id, Map.of("newName", renamed), adminCookie, OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(departmentId(renamed)).isEqualTo(id);

        // 删除空部门
        assertThat(delete("/api/v1/departments/" + id, adminCookie, OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void uc18_defaultDepartmentCannotBeRenamedOrDeleted() {
        Long defaultId = departmentMapper.getDefault().getId();
        ResponseEntity<ApiError> rename = put("/api/v1/departments/" + defaultId,
                Map.of("newName", "nope"), adminCookie, ApiError.class);
        assertThat(rename.getBody().getCode()).isEqualTo("CANNOT_RENAME_DEFAULT_DEPT");

        ResponseEntity<ApiError> delete = delete("/api/v1/departments/" + defaultId, adminCookie, ApiError.class);
        assertThat(delete.getBody().getCode()).isEqualTo("CANNOT_DELETE_DEFAULT_DEPT");
    }

    @Test
    void uc18_nonEmptyDepartmentCannotBeDeleted() {
        String name = "Dept-nonempty-" + uniqueSuffix();
        Long id = createDepartment(name);
        createTicket(cookie(USER_EMAILS[0]), id, "UC18 nonempty " + uniqueSuffix());

        ResponseEntity<ApiError> delete = delete("/api/v1/departments/" + id, adminCookie, ApiError.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(delete.getBody().getCode()).isEqualTo("DEPT_NOT_EMPTY");
    }

    @Test
    void uc18_migrateTickets_unsassignsAndMoves() {
        Long src = createDepartment("Dept-src-tickets-" + uniqueSuffix());
        Long dst = createDepartment("Dept-dst-tickets-" + uniqueSuffix());
        TicketDetailView ticket = createTicket(cookie(USER_EMAILS[0]), src, "UC18 migrate " + uniqueSuffix());

        assertThat(post("/api/v1/departments/" + src + "/migrate-tickets",
                Map.of("targetDepartmentId", dst), adminCookie, OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        TicketDetailView moved = get("/api/v1/tickets/" + ticket.getId(), adminCookie, TicketDetailView.class).getBody();
        assertThat(moved.getDepartmentId()).isEqualTo(dst);
        assertThat(moved.getAssigneeId()).isNull();
    }

    @Test
    void uc18_migrateStaff_movesAndUnassignsTheirTickets() {
        Long src = createDepartment("Dept-src-staff-" + uniqueSuffix());
        Long dst = createDepartment("Dept-dst-staff-" + uniqueSuffix());
        String staffEmail = "mig-staff-" + uniqueSuffix() + "@test.local";
        createStaff(staffEmail, 1, src);
        Long staffId = userId(staffEmail);

        TicketDetailView ticket = createTicket(cookie(USER_EMAILS[0]), src, "UC18 migrate staff " + uniqueSuffix());
        put("/api/v1/tickets/" + ticket.getId() + "/assign", Map.of("staffId", staffId), adminCookie, OperationResult.class);

        assertThat(put("/api/v1/departments/" + src + "/migrate-staff",
                Map.of("targetDepartmentId", dst), adminCookie, OperationResult.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        DetailedStaffView staff = get("/api/v1/admin/staff/" + staffId, adminCookie, DetailedStaffView.class).getBody();
        assertThat(staff.getDepartmentId()).isEqualTo(dst);
        TicketDetailView reloaded = get("/api/v1/tickets/" + ticket.getId(), adminCookie, TicketDetailView.class).getBody();
        assertThat(reloaded.getAssigneeId()).isNull();
    }

    @Test
    void uc18_requiresAdmin() {
        ResponseEntity<ApiError> resp = post("/api/v1/departments",
                Map.of("name", "Dept-l1-" + uniqueSuffix()), cookie(L1_EMAILS[0]), ApiError.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
