package org.fit.shopnuochoa.component;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * Kiểm tra xem người dùng hiện tại có role nhất định hay không
     */
    public static boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + roleName));
    }
}
