package org.fit.shopnuochoa.controller;

import jakarta.servlet.http.HttpSession;
import org.fit.shopnuochoa.model.CartBean;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/cart") // Đổi thành /cart để ngắn gọn hơn
public class CartController {
    private final ProductService productService;

    public CartController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Lấy giỏ hàng từ session. Nếu chưa có, tạo mới.
     * @param session HttpSession hiện tại
     * @return CartBean object
     */
    private CartBean getCart(HttpSession session) {
        CartBean cart = (CartBean) session.getAttribute("cart");
        if (cart == null) {
            cart = new CartBean();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    /**
     * Hiển thị trang giỏ hàng
     */
    @GetMapping
    public String showCart(HttpSession session, Model model) {
        CartBean cart = getCart(session);
        model.addAttribute("cart", cart);
        return "screen/customer/cart"; // Trả về file view hiển thị giỏ hàng
    }

    /**
     * Xử lý các hành động thêm, sửa, xóa sản phẩm trong giỏ hàng
     */
    @PostMapping
    public String handleCartAction(@RequestParam("action") String action,
                                   @RequestParam(value = "productId", required = false) Integer productId,
                                   @RequestParam(value = "quantity", required = false) Integer quantity,
                                   HttpSession session, RedirectAttributes redirectAttributes) {



        CartBean cart = getCart(session);

        try {
            switch (action) {
                case "add":
                    if (productId != null) {
                        Product p = productService.getById(productId);
                        if (p != null) {
                            if (p.getInStock()) {
                                cart.addProduct(p);
                                // Gửi thông báo thành công
                                redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
                            } else {
                                // 3. Gửi thông báo lỗi khi sản phẩm hết hàng
                                redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm \"" + p.getName() + "\" đã hết hàng!");
                            }
                        }
                    }
                    break;
                case "update":
                    if (productId != null && quantity != null) {
                        cart.updateQuantity(productId, quantity);
                    }
                    break;
                case "remove":
                    if (productId != null) {
                        cart.removeProduct(productId);
                    }
                    break;
                case "clear":
                    cart.clear();
                    break;
            }
        } catch (Exception e) {
            // Xử lý lỗi nếu cần
            e.printStackTrace();
        }

        // Chuyển hướng về trang giỏ hàng sau khi thực hiện hành động
        return "redirect:/api/cart";
    }
}