package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @PostMapping("/toggle")
    public String toggleWishlist(@RequestParam("productId") int productId,
                                 @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/api/login"; // Yêu cầu đăng nhập
        }

        // [SỬA ĐỔI] Gọi hàm mới để lấy Customer ID
        Integer customerId = wishlistService.getCustomerIdByUsername(user.getUsername());

        // Nếu không tìm thấy (ví dụ: user là Admin hoặc lỗi), không làm gì
        if (customerId == null) {
            // Hoặc bạn có thể thêm thông báo lỗi
            return "redirect:/api/products/detail/" + productId;
        }

        // [SỬA ĐỔI] Gọi hàm toggleWishlist bằng customerId
        wishlistService.toggleWishlist(customerId, productId);

        // Chuyển hướng lại trang chi tiết
        return "redirect:/api/products/detail/" + productId;
    }
}