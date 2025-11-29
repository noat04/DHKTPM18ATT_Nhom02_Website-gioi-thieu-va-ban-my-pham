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

    /// Bên trong file CartController.java

    /**
     * Xử lý các hành động thêm, sửa, xóa sản phẩm trong giỏ hàng
     */
    @PostMapping
    public String handleCartAction(@RequestParam("action") String action,
                                   @RequestParam(value = "productId", required = false) Integer productId,
                                   @RequestParam(value = "quantity", required = false) Integer quantity,
                                   HttpSession session, RedirectAttributes redirectAttributes) {

        CartBean cart = getCart(session);
        Product product;

        // DÙNG try-catch ĐỂ BẮT CÁC LỖI TỒN KHO TỪ CARTBEAN
        try {
            switch (action) {
                case "add":
                    if (productId == null) break;
                    product = productService.getById(productId);
                    if (product == null) break;

                    // Lấy số lượng muốn thêm (mặc định là 1 nếu không có)
                    int quantityToAdd = (quantity != null && quantity > 0) ? quantity : 1;

                    // Logic gọi addProduct N lần
                    // (Nếu 1 trong các lần gọi thất bại, nó sẽ bị bắt ở catch)
                    for (int i = 0; i < quantityToAdd; i++) {
                        cart.addProduct(product);
                    }
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Đã thêm " + quantityToAdd + " sản phẩm \"" + product.getName() + "\" vào giỏ!");
                    break;

                case "update":
                    if (productId == null || quantity == null) break;

                    // Không cần kiểm tra tồn kho ở đây nữa,
                    // vì cart.updateQuantity() (trong CartBean) sẽ tự làm
                    cart.updateQuantity(productId, quantity);

                    redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật số lượng.");
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
        } catch (RuntimeException e) {
            // === PHẦN SỬA LỖI QUAN TRỌNG ===
            // Bắt lỗi (ví dụ: "Không đủ hàng!") và gửi cho người dùng
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        } catch (Exception e) {
            // Bắt các lỗi chung khác
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/api/cart";
    }
}