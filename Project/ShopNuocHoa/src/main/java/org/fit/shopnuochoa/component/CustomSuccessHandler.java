package org.fit.shopnuochoa.component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // Lấy danh sách quyền (role) của user
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        // Chuyển hướng theo role
        if (roles.contains("ROLE_ADMIN")) {
            response.sendRedirect("/api/dashboard");
        } else {
            response.sendRedirect("/api/home");
        }
    }
}