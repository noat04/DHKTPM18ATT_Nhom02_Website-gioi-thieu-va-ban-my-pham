package org.fit.shopnuochoa.controller;

import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.model.CartBean;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalController {

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
}