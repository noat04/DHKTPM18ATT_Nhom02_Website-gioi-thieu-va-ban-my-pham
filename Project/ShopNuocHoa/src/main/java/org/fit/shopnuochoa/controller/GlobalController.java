package org.fit.shopnuochoa.controller;

import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalController {
    @Autowired
    private UserService userService;
    /**
     * Phương thức này sẽ tự động thêm đối tượng 'cart' vào Model cho mọi trang.
     * Nhờ đó, file header.html có thể truy cập ${cart} ở bất kỳ đâu.
     */
    @ModelAttribute("cart")
    public CartBean getCart(HttpSession session) {
        CartBean cart = (CartBean) session.getAttribute("cart");
        if (cart == null) {
            cart = new CartBean();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    /**
     * Hàm này sẽ tự động thêm "currentUser" vào Model của TẤT CẢ các trang
     * Giúp gọi ${currentUser.avatarPath} ở header, footer, hay bất kỳ đâu.
     */
    @ModelAttribute("currentUser")
    public Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra nếu người dùng đã đăng nhập và không phải là anonymous
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            String username = authentication.getName();
            // Lấy thông tin mới nhất từ DB (để cập nhật avatar ngay khi thay đổi)
            return userService.getUserByUsername(username);
        }

        return null; // Chưa đăng nhập
    }
}