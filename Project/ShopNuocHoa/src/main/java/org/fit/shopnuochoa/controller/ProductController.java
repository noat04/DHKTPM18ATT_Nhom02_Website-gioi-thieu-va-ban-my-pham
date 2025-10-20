package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.component.SecurityUtils;
import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.model.Comment;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.service.CategoryService;
import org.fit.shopnuochoa.service.CommentService;
import org.fit.shopnuochoa.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequestMapping("/api/products")
public class ProductController {
    private SecurityUtils securityUtils;
    private ProductService productService;
    private CategoryService categoryService;
    private CommentService commentService;
    public ProductController(ProductService productService, CategoryService categoryService,CommentService commentService,SecurityUtils securityUtils) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.commentService = commentService;
        this.securityUtils = securityUtils;
    }
    // ✅ Đúng
    @GetMapping("/list")
    public String showProductList(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "price", required = false) Double salary,
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size,
            Model model,
            Authentication authentication) {

        // ✅ Lấy danh sách loại sản phẩm
        List<Category> categories = categoryService.getAll();

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = null;

        if (action == null) action = "list";

        switch (action) {
            case "add":
                productPage = productService.getAll(pageable);
                break;
            case "list":
                if (categoryId != null && salary != null && salary > 0) {
                    productPage = productService.getProductsByCategoryWithPriceGreaterThan(categoryId, salary, pageable);
                } else if (categoryId != null) {
                    productPage = productService.getByCategory(categoryId, pageable);
                } else if (salary != null && salary > 0) {
                    productPage = productService.getByPrice(salary, pageable);
                } else {
                    productPage = productService.getAll(pageable);
                }
                break;
            case "delete":
                // ✅ Chỉ admin mới được xóa
                if (authentication != null && authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                    productService.deleteProduct(id);
                }
                productPage = productService.getAll(pageable);
                break;
            default:
                System.out.println("Unknown action: " + action);
                break;
        }

        // ✅ Gửi dữ liệu sang view
        model.addAttribute("categories", categories);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("price", salary);
        model.addAttribute("productPage", productPage);

        // ✅ Kiểm tra trạng thái đăng nhập
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            // Người chưa đăng nhập (khách)
            return "screen/customer/product-list";
        }

        // ✅ Nếu đã đăng nhập, kiểm tra role
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return "screen/admin/admin-product-list";
        } else {
            return "screen/customer/product-list";
        }
    }



    // ✅ Đúng
    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String showEmployeeForm(@RequestParam(value = "action", required = false, defaultValue = "add") String action,
                                   @RequestParam(value = "id", required = false) Integer id,
                                   Model model) {
        Product product;
        List<Category> categories = categoryService.getAll();
        if ("edit".equals(action) && id != null) {
            product = productService.getById(id); // Lấy thông tin nhân viên theo id
        } else {
            product = new Product(); // Thêm mới thì tạo mới
        }
        model.addAttribute("categories", categories);
        model.addAttribute("product", product);
        model.addAttribute("action", action);
        return "screen/admin/admin-product-form";
    }


    @PostMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String handleEmployeeForm(@RequestParam("action") String action,
                                     @RequestParam(value = "categoryId", required = false) Integer categoryId,
                                     Product product) {
        if ("add".equals(action)) {
            productService.createProduct(product, categoryId);
        } else if ("edit".equals(action)) {
            productService.updateProduct(product.getId(), product, categoryId);
        }
        return "redirect:/api/products/list";
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public String showDetail(@PathVariable("id") Integer id,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "5") int size,
                             Model model) {
        Product product = productService.getById(id);
        model.addAttribute("product", product);

        if (product != null) {
            // Lấy danh sách bình luận (với phân trang riêng)
            Pageable commentPageable = PageRequest.of(page, size);
            Page<Comment> commentPage = commentService.getByProductId(id, commentPageable);
            model.addAttribute("commentPage", commentPage);
            model.addAttribute("productId", id);

            // ✅ Lấy danh sách sản phẩm tương tự (không phân trang)
            List<Product> similarProducts = productService.findSimilarProducts(
                    product.getCategory().getId(),
                    id);
            model.addAttribute("similarProducts", similarProducts);
        }

        return "screen/customer/product-detail";
    }

}
