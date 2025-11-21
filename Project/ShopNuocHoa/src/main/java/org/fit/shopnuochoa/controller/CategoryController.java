package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.component.SecurityUtils;
import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.service.CategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/api/categories")
public class CategoryController {
    private SecurityUtils securityUtils;
    private CategoryService categoryService;
    public CategoryController(CategoryService categoryService,SecurityUtils securityUtils) {
        this.categoryService = categoryService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/list")
// @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')") // ❌ Bỏ vì cần hiển thị cả khi chưa đăng nhập
    public String showCategoryList(
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "id", required = false) Integer id,
            Model model,
            Authentication authentication) {

        // ✅ Nếu là ADMIN và có hành động delete
        if ("delete".equals(action) && id != null && SecurityUtils.hasRole(authentication, "ADMIN")) {
            categoryService.deleteCategory(id);
            return "redirect:/api/categories/list";
        }

        // ✅ Lấy danh sách tất cả category
        List<Category> categories = categoryService.getAll();
        model.addAttribute("categories", categories);

        // ✅ Nếu chưa đăng nhập hoặc là CUSTOMER → hiển thị customer view
        if (authentication == null || SecurityUtils.hasRole(authentication, "CUSTOMER")) {
            return "screen/customer/category-list";
        }

        // ✅ Nếu là ADMIN → hiển thị admin view
        if (SecurityUtils.hasRole(authentication, "ADMIN")) {
            return "screen/admin/admin-category-list";
        }

        // ✅ Mặc định (phòng lỗi)
        return "screen/customer/category-list";
    }


    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String showDepartmentForm(@RequestParam(value = "action", required = false, defaultValue = "add") String action,
                                     @RequestParam(value = "id", required = false) Integer id,
                                     Model model) {
        Category category;

        if ("edit".equals(action) && id != null) {
            // Nếu là chỉnh sửa, lấy thông tin phòng ban từ DB
            category = categoryService.getById(id);
        } else {
            // Nếu là thêm mới, tạo một đối tượng rỗng
            category = new Category();
        }

        model.addAttribute("category", category);
        model.addAttribute("action", action); // Truyền action để form biết là 'add' hay 'edit'
        return "screen/admin/admin-category-form"; // Trả về file view department-form.html
    }

    /**
     * Xử lý dữ liệu được gửi từ form (thêm mới hoặc cập nhật).
     */
    @PostMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String handleDepartmentForm(@RequestParam("action") String action,
                                       @RequestParam("imageFile") MultipartFile imageFile, // <-- THÊM NHẬN FILE,
                                       Category category) { // Spring tự động binding dữ liệu từ form vào đối tượng

        if ("add".equals(action)) {
            categoryService.createCategory(category, imageFile);
        } else if ("edit".equals(action)) {
            categoryService.updateCategory(category.getId(), category, imageFile);
        }

        return "redirect:/api/categories/list"; // Chuyển hướng về trang danh sách sau khi xử lý
    }

}
