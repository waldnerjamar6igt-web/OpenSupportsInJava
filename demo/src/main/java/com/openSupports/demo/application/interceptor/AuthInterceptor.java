package com.openSupports.demo.application.interceptor;

import com.openSupports.demo.Domain.Model.Account;
import com.openSupports.demo.Domain.repo.AccountRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 认证拦截器。
 *
 * <p>公开端点（注册/登录/知识库浏览）放行；其余 /api/v1/** 需要有效会话。
 * 登录用户会把 currentUserId / currentUserRole / currentUserDeptId 写入 request 属性。</p>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> PUBLIC_POST = Set.of(
            "/api/v1/auth/signup",
            "/api/v1/auth/signin"
    );

    private final AccountRepo accountRepo;

    public AuthInterceptor(AccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        HttpSession session = request.getSession(false);
        Long userId = session != null ? (Long) session.getAttribute("userId") : null;
        Account account = userId != null ? accountRepo.findById(userId) : null;

        if (account != null) {
            populate(request, account);
        }

        if (isPublic(method, path)) {
            return true;
        }

        if (account == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "会话已过期，请重新登录");
            return false;
        }
        if (!account.isEnabled()) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "ACCOUNT_DISABLED", "账户已被停用");
            return false;
        }
        return true;
    }

    private boolean isPublic(String method, String path) {
        if ("POST".equals(method) && PUBLIC_POST.contains(path)) {
            return true;
        }
        if ("GET".equals(method)) {
            return path.equals("/api/v1/kb/folders")
                    || path.startsWith("/api/v1/kb/articles");
        }
        return false;
    }

    private void populate(HttpServletRequest request, Account account) {
        request.setAttribute("currentUserId", account.getId());
        request.setAttribute("currentUserRole", account.isStaff() ? "STAFF_" + account.getLevel() : "USER");
        request.setAttribute("currentUserDeptId", account.getDepartmentId());
        request.setAttribute("currentUserEmail", account.getEmail());
    }

    private void sendError(HttpServletResponse response, int status, String code, String msg) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + msg + "\"}");
    }
}
