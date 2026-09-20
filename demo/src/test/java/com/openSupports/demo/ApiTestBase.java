package com.openSupports.demo;

import com.openSupports.demo.Domain.Model.Department;
import com.openSupports.demo.Domain.Model.Staff;
import com.openSupports.demo.Domain.Model.User;
import com.openSupports.demo.api.dto.staff.CreateStaffCmd;
import com.openSupports.demo.api.dto.staff.CreateUserCmd;
import com.openSupports.demo.api.dto.department.CreateDeptCmd;
import com.openSupports.demo.api.dto.department.DepartmentOverviewView;
import com.openSupports.demo.api.dto.ticket.CreateTicketCmd;
import com.openSupports.demo.api.dto.ticket.TicketDetailView;
import com.openSupports.demo.api.dto.user.TokenResp;
import com.openSupports.demo.infra.mapper.DepartmentMapper;
import com.openSupports.demo.infra.mapper.StafaMapper;
import com.openSupports.demo.infra.mapper.TagMapper;
import com.openSupports.demo.infra.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 所有 BA.md API 集成测试的基类。
 *
 * <p>使用真实 HTTP（RANDOM_PORT）+ SQLite 文件库，通过 Spring Session 的 cookie 维持登录态。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ApiTestBase {

    protected static final Path DB_PATH = Path.of("target", "opensupports-test.sqlite").toAbsolutePath();

    static {
        try {
            Files.deleteIfExists(DB_PATH);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB_PATH);
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "1");
        registry.add("mybatis.configuration.log-impl", () -> "org.apache.ibatis.logging.nologging.NoLoggingImpl");
    }

    @Autowired
    protected UserMapper userMapper;
    @Autowired
    protected StafaMapper stafaMapper;
    @Autowired
    protected DepartmentMapper departmentMapper;
    @Autowired
    protected TagMapper tagMapper;

    @LocalServerPort
    protected int port;

    protected final RestTemplate rest = new RestTemplate();

    protected ApiTestBase() {
        rest.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(java.net.URI url, HttpMethod method, ClientHttpResponse response) {
                // 不抛异常，测试自行断言响应状态码
            }
        });
    }

    protected static final String ADMIN_EMAIL = "admin@opensupports.test";
    protected static final String ADMIN_PASSWORD = "password123";
    protected static final String DEFAULT_PASSWORD = "password123";

    // 需求：user 2 个，staff L1/L2/L3 各 2 个
    protected static final String[] USER_EMAILS = {"user1@opensupports.test", "user2@opensupports.test"};
    protected static final String[] L1_EMAILS = {"l1a@opensupports.test", "l1b@opensupports.test"};
    protected static final String[] L2_EMAILS = {"l2a@opensupports.test", "l2b@opensupports.test"};
    protected static final String[] L3_EMAILS = {"l3a@opensupports.test", "l3b@opensupports.test"};

    protected static volatile boolean worldReady = false;

    protected String adminCookie;
    protected static Long deptAId;
    protected static Long deptBId;

    protected final Map<String, String> cookieByEmail = new HashMap<>();
    protected final Map<String, Long> idByEmail = new HashMap<>();

    @BeforeAll
    void bootstrapWorld() {
        ensureWorld();
    }

    protected synchronized void ensureWorld() {
        if (!worldReady) {
            seedDefaultDepartmentAndAdmin();
            adminCookie = signin(ADMIN_EMAIL, ADMIN_PASSWORD);
            seedDepartmentsViaApi();
            seedActorsViaApi();
            worldReady = true;
        } else if (adminCookie == null) {
            adminCookie = signin(ADMIN_EMAIL, ADMIN_PASSWORD);
        }
        if (cookieByEmail.isEmpty()) {
            for (String email : USER_EMAILS) {
                loginAndRemember(email);
            }
            for (String email : allStaffEmails()) {
                loginAndRemember(email);
            }
        }
    }

    // ------------------------------------------------------------------
    // Seeding
    // ------------------------------------------------------------------
    private void seedDefaultDepartmentAndAdmin() {
        if (departmentMapper.getDefault() == null) {
            Department dept = new Department();
            dept.setName("Software Support");
            dept.setIsDefault(true);
            dept.setIsPrivate(false);
            departmentMapper.insert(dept);
        }
        if (userMapper.findByEmail(ADMIN_EMAIL) == null) {
            String salt = UUID.randomUUID().toString();
            String hash = BCrypt.hashpw(ADMIN_PASSWORD + salt, BCrypt.gensalt());
            User user = new User();
            user.setEmail(ADMIN_EMAIL);
            user.setPasswordHash(hash);
            user.setSalt(salt);
            user.setState("ENABLED");
            user.setCsrfUserid("");
            user.setCsrfToken("");
            userMapper.insert(user);

            Staff staff = new Staff();
            staff.setId(user.getId());
            staff.setLevel(3);
            staff.setDepartmentId(departmentMapper.getDefault().getId());
            staff.setState("ENABLED");
            stafaMapper.insertStaff(staff);
        }
    }

    private void seedDepartmentsViaApi() {
        deptAId = ensureDepartment("General Support");
        deptBId = ensureDepartment("Billing Support");
    }

    private Long ensureDepartment(String name) {
        List<DepartmentOverviewView> depts = Arrays.asList(
                get("/api/v1/departments", adminCookie, DepartmentOverviewView[].class).getBody());
        for (DepartmentOverviewView d : depts) {
            if (name.equals(d.getName())) {
                return d.getId();
            }
        }
        ResponseEntity<String> created = post("/api/v1/departments", dept(name), adminCookie, String.class);
        if (!created.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Failed to seed department " + name + ": " + created.getStatusCode() + " " + created.getBody());
        }
        depts = Arrays.asList(get("/api/v1/departments", adminCookie, DepartmentOverviewView[].class).getBody());
        return depts.stream().filter(d -> name.equals(d.getName())).findFirst().orElseThrow().getId();
    }

    private CreateDeptCmd dept(String name) {
        CreateDeptCmd cmd = new CreateDeptCmd();
        cmd.setName(name);
        return cmd;
    }

    private void seedActorsViaApi() {
        for (String email : USER_EMAILS) {
            if (userMapper.findByEmail(email) == null) {
                CreateUserCmd cmd = new CreateUserCmd();
                cmd.setEmail(email);
                cmd.setPassword(DEFAULT_PASSWORD);
                post("/api/v1/admin/users", cmd, adminCookie, String.class);
            }
        }
        for (String email : L1_EMAILS) {
            ensureStaff(email, 1, deptAId);
        }
        for (String email : L2_EMAILS) {
            ensureStaff(email, 2, deptAId);
        }
        ensureStaff(L3_EMAILS[0], 3, deptAId);
        ensureStaff(L3_EMAILS[1], 3, deptBId);
    }

    private void ensureStaff(String email, int level, Long deptId) {
        if (stafaMapper.findByEmail(email) != null) {
            return;
        }
        CreateStaffCmd cmd = new CreateStaffCmd();
        cmd.setEmail(email);
        cmd.setPassword(DEFAULT_PASSWORD);
        cmd.setLevel(level);
        cmd.setDepartmentId(deptId);
        ResponseEntity<String> resp = post("/api/v1/admin/staff", cmd, adminCookie, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Failed to seed staff " + email + ": " + resp.getStatusCode() + " " + resp.getBody());
        }
    }

    protected String[] allStaffEmails() {
        String[] all = new String[L1_EMAILS.length + L2_EMAILS.length + L3_EMAILS.length];
        System.arraycopy(L1_EMAILS, 0, all, 0, L1_EMAILS.length);
        System.arraycopy(L2_EMAILS, 0, all, L1_EMAILS.length, L2_EMAILS.length);
        System.arraycopy(L3_EMAILS, 0, all, L1_EMAILS.length + L2_EMAILS.length, L3_EMAILS.length);
        return all;
    }

    // ------------------------------------------------------------------
    // HTTP helpers
    // ------------------------------------------------------------------
    protected HttpHeaders headers(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        return headers;
    }

    protected <T> ResponseEntity<T> exchange(HttpMethod method, String path, Object body, String cookie, Class<T> type) {
        HttpEntity<Object> entity = new HttpEntity<>(body, headers(cookie));
        return rest.exchange("http://localhost:" + port + path, method, entity, type);
    }

    protected <T> ResponseEntity<T> get(String path, String cookie, Class<T> type) {
        return exchange(HttpMethod.GET, path, null, cookie, type);
    }

    protected <T> ResponseEntity<T> post(String path, Object body, String cookie, Class<T> type) {
        return exchange(HttpMethod.POST, path, body, cookie, type);
    }

    protected <T> ResponseEntity<T> put(String path, Object body, String cookie, Class<T> type) {
        return exchange(HttpMethod.PUT, path, body, cookie, type);
    }

    protected <T> ResponseEntity<T> delete(String path, String cookie, Class<T> type) {
        return exchange(HttpMethod.DELETE, path, null, cookie, type);
    }

    protected <T> ResponseEntity<T> getType(String path, String cookie, ParameterizedTypeReference<T> type) {
        return rest.exchange("http://localhost:" + port + path, HttpMethod.GET, new HttpEntity<>(headers(cookie)), type);
    }

    protected TicketDetailView createTicket(String cookie, long deptId, String title) {
        CreateTicketCmd cmd = new CreateTicketCmd();
        cmd.setTitle(title);
        cmd.setContent("content of " + title);
        cmd.setDepartmentId(deptId);
        ResponseEntity<TicketDetailView> resp = post("/api/v1/tickets", cmd, cookie, TicketDetailView.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Failed to create ticket: " + resp.getStatusCode());
        }
        return resp.getBody();
    }

    protected String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected String signin(String email, String password) {
        ResponseEntity<TokenResp> resp = post("/api/v1/auth/signin",
                Map.of("email", email, "password", password, "rememberMe", false),
                null, TokenResp.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            return null;
        }
        idByEmail.put(email, Long.valueOf(resp.getBody().getUserId()));
        return extractSessionCookie(resp.getHeaders());
    }

    protected String loginAndRemember(String email) {
        String cookie = signin(email, DEFAULT_PASSWORD);
        cookieByEmail.put(email, cookie);
        return cookie;
    }

    protected String cookie(String email) {
        return cookieByEmail.get(email);
    }

    protected Long userId(String email) {
        if (!idByEmail.containsKey(email)) {
            loginAndRemember(email);
        }
        if (idByEmail.containsKey(email)) {
            return idByEmail.get(email);
        }
        User u = userMapper.findByEmail(email);
        if (u != null) {
            idByEmail.put(email, u.getId());
            return u.getId();
        }
        Staff s = stafaMapper.findByEmail(email);
        return s != null ? s.getId() : null;
    }

    protected String extractSessionCookie(HttpHeaders responseHeaders) {
        List<String> cookies = responseHeaders.get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }
        for (String cookie : cookies) {
            if (cookie.startsWith("SESSION=") || cookie.startsWith("JSESSIONID=")) {
                return cookie.split(";", 2)[0];
            }
        }
        return cookies.get(0).split(";", 2)[0];
    }
}
