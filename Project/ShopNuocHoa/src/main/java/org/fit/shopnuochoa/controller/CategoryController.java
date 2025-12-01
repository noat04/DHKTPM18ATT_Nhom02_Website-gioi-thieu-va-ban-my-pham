package org.fit.shopnuochoa.controller;

import jakarta.validation.Valid;
import org.fit.shopnuochoa.component.SecurityUtils;
import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            Model model,
            Authentication authentication) {

        // ✅ Nếu là ADMIN và có hành động delete
        if ("delete".equals(action) && id != null && SecurityUtils.hasRole(authentication, "ADMIN")) {
            categoryService.deleteCategory(id);
            return "redirect:/api/categories/list";
        }

        // ✅ Nếu là ADMIN → hiển thị admin view với phân trang
        if (SecurityUtils.hasRole(authentication, "ADMIN")) {
            Pageable pageable = PageRequest.of(page, 10); // 10 items per page
            Page<Category> categoryPage;

            if (keyword != null && !keyword.trim().isEmpty()) {
                // Tìm kiếm theo tên
                categoryPage = categoryService.searchByName(keyword.trim(), pageable);
            } else {
                // Lấy tất cả
                categoryPage = categoryService.getAllPaged(pageable);
            }

            model.addAttribute("categoryPage", categoryPage);
            model.addAttribute("keyword", keyword);
            return "screen/admin/admin-category-list";
        }

        // ✅ Nếu chưa đăng nhập hoặc là CUSTOMER → hiển thị customer view (không phân trang)
        List<Category> categories = categoryService.getAll();
        model.addAttribute("categories", categories);

        if (authentication == null || SecurityUtils.hasRole(authentication, "CUSTOMER")) {
            return "screen/customer/category-list";
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
                                       @Valid @ModelAttribute("category") Category category, // [QUAN TRỌNG] Thêm @Valid
                                       BindingResult result, // [QUAN TRỌNG] Chứa kết quả kiểm tra lỗi
                                       RedirectAttributes redirectAttributes, // Để gửi thông báo thành công sau khi redirect
                                       Model model) { // Spring tự động binding dữ liệu từ form vào đối tượng

        // 1. Kiểm tra lỗi Validation (Annotation trong Entity)
        if (result.hasErrors()) {
            // Nếu có lỗi, trả về trang form để hiển thị lỗi ngay lập tức
            // (Không redirect, giữ nguyên dữ liệu người dùng đã nhập)
            return "screen/admin/admin-category-form";
        }

        try {
            // 2. Xử lý logic nghiệp vụ
            if ("edit".equals(action) || (category.getId() != null && category.getId() > 0)) {
                // Logic cập nhật
                categoryService.updateCategory(category.getId(), category, imageFile);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
            } else {
                // Logic thêm mới
                categoryService.createCategory(category, imageFile);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm mới danh mục thành công!");
            }

        } catch (Exception e) {
            // 3. Bắt lỗi logic (Ví dụ: Trùng tên danh mục, lỗi lưu file ảnh)
            e.printStackTrace();
            model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "screen/admin/admin-category-form"; // Trả về form để báo lỗi
        }

        // 4. Thành công -> Chuyển hướng về danh sách
        return "redirect:/api/categories/list";
    }

}
