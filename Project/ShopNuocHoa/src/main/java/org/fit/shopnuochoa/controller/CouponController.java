package org.fit.shopnuochoa.controller;

import jakarta.validation.Valid;
import org.fit.shopnuochoa.model.*;
import org.fit.shopnuochoa.service.CategoryService;
import org.fit.shopnuochoa.service.CloudinaryService; // Import CloudinaryService
import org.fit.shopnuochoa.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // Import MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/api/coupons")
@PreAuthorize("hasRole('ADMIN')")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CloudinaryService cloudinaryService;

    /**
     * 1. HIỂN THỊ DANH SÁCH COUPON
     */
    @GetMapping("/list")
    public String listCoupons(Model model) {
        List<Coupon> coupons = couponService.getAll();
        model.addAttribute("coupons", coupons);
        return "screen/admin/admin-coupon-list";
    }

    /**
     * 2. HIỂN THỊ FORM THÊM MỚI
     */
    @GetMapping("/create")
    public String showCreateForm(@RequestParam(required = false) String type, Model model) {
        Coupon coupon;
        if (type == null) type = "ORDER_TOTAL";

        switch (type) {
            case "CATEGORY_SPECIFIC": coupon = new CategoryCoupon(); break;
            case "WELCOME": coupon = new WelcomeCoupon(); break;
            default: coupon = new OrderCoupon(); break;
        }

        model.addAttribute("coupon", coupon);
        model.addAttribute("type", type);
        model.addAttribute("categories", categoryService.getAll());

        return "screen/admin/admin-coupon-form";
    }

    /**
     * 3. HIỂN THỊ FORM SỬA
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Coupon coupon = couponService.getById(id);
        String type = getCouponTypeString(coupon);

        model.addAttribute("coupon", coupon);
        model.addAttribute("type", type);
        model.addAttribute("categories", categoryService.getAll());

        return "screen/admin/admin-coupon-form";
    }

    /**
     * 4. XỬ LÝ LƯU (THÊM/SỬA)
     * Thêm tham số @RequestParam("imageFile") MultipartFile imageFile
     */

    // --- Xử lý OrderCoupon ---
    @PostMapping("/save/order")
    public String saveOrderCoupon(@Valid @ModelAttribute("coupon") OrderCoupon coupon, // [1] @Valid
                                  BindingResult result, // [2] BindingResult
                                  @RequestParam("imageFile") MultipartFile imageFile,
                                  RedirectAttributes ra,
                                  Model model) {

        //Check lỗi Validate
        if (result.hasErrors()) {
            model.addAttribute("type", "ORDER_TOTAL");
            model.addAttribute("categories", categoryService.getAll()); // Load lại danh mục để form không bị lỗi
            return "screen/admin/admin-coupon-form"; // Trả về form để hiện lỗi
        }

        try {
            handleImageUpload(coupon, imageFile);

            if (coupon.getId() != null) {
                couponService.update(coupon.getId(), coupon);
                ra.addFlashAttribute("successMessage", "Cập nhật mã giảm giá thành công!");
            } else {
                couponService.create(coupon);
                ra.addFlashAttribute("successMessage", "Thêm mới mã giảm giá thành công!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Lỗi logic thì trả về form báo lỗi
            model.addAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            model.addAttribute("type", "ORDER_TOTAL");
            model.addAttribute("categories", categoryService.getAll());
            return "screen/admin/admin-coupon-form";
        }
        return "redirect:/api/coupons/list";
    }

    // --- Xử lý CategoryCoupon ---
    @PostMapping("/save/category")
    public String saveCategoryCoupon(@Valid @ModelAttribute("coupon") CategoryCoupon coupon,
                                     BindingResult result,
                                     @RequestParam("imageFile") MultipartFile imageFile,
                                     RedirectAttributes ra,
                                     Model model) {

        if (result.hasErrors()) {
            model.addAttribute("type", "CATEGORY_SPECIFIC");
            model.addAttribute("categories", categoryService.getAll());
            return "screen/admin/admin-coupon-form";
        }

        try {
            handleImageUpload(coupon, imageFile);

            if (coupon.getId() != null) {
                couponService.update(coupon.getId(), coupon);
                ra.addFlashAttribute("successMessage", "Cập nhật mã giảm giá thành công!");
            } else {
                couponService.create(coupon);
                ra.addFlashAttribute("successMessage", "Thêm mới mã giảm giá thành công!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            model.addAttribute("type", "CATEGORY_SPECIFIC");
            model.addAttribute("categories", categoryService.getAll());
            return "screen/admin/admin-coupon-form";
        }
        return "redirect:/api/coupons/list";
    }

    // --- Xử lý WelcomeCoupon ---
    @PostMapping("/save/welcome")
    public String saveWelcomeCoupon(@Valid @ModelAttribute("coupon") WelcomeCoupon coupon,
                                    BindingResult result,
                                    @RequestParam("imageFile") MultipartFile imageFile,
                                    RedirectAttributes ra,
                                    Model model) {

        if (result.hasErrors()) {
            model.addAttribute("type", "WELCOME");
            model.addAttribute("categories", categoryService.getAll());
            return "screen/admin/admin-coupon-form";
        }

        try {
            handleImageUpload(coupon, imageFile);

            if (coupon.getId() != null) {
                couponService.update(coupon.getId(), coupon);
                ra.addFlashAttribute("successMessage", "Cập nhật mã giảm giá thành công!");
            } else {
                couponService.create(coupon);
                ra.addFlashAttribute("successMessage", "Thêm mới mã giảm giá thành công!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            model.addAttribute("type", "WELCOME");
            model.addAttribute("categories", categoryService.getAll());
            return "screen/admin/admin-coupon-form";
        }
        return "redirect:/api/coupons/list";
    }

    /**
     * 5. XÓA COUPON (Kèm xóa ảnh trên Cloudinary nếu cần)
     */
    @GetMapping("/delete/{id}")
    public String deleteCoupon(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            Coupon coupon = couponService.getById(id);

            // Xóa ảnh trên Cloudinary trước khi xóa DB (Tùy chọn, để tiết kiệm dung lượng)
            if(coupon.getImageUrl() != null && coupon.getImageUrl().startsWith("http")) {
                // Cần thêm hàm xóa public vào CloudinaryService nếu muốn dùng ở đây
                 cloudinaryService.deleteImageByUrl(coupon.getImageUrl());
            }

            couponService.delete(id);
            ra.addFlashAttribute("successMessage", "Xóa mã giảm giá thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/api/coupons/list";
    }

    // --- Helper Methods ---

    private String getCouponTypeString(Coupon c) {
        if (c instanceof OrderCoupon) return "ORDER_TOTAL";
        if (c instanceof CategoryCoupon) return "CATEGORY_SPECIFIC";
        return "WELCOME";
    }

    /**
     * Hàm xử lý logic upload ảnh chung cho mọi loại coupon
     */
    private void handleImageUpload(Coupon coupon, MultipartFile imageFile) {
        if (!imageFile.isEmpty()) {
            // Nếu là cập nhật (đã có ID và ảnh cũ) -> Update (Xóa cũ up mới)
            if (coupon.getId() != null && coupon.getImageUrl() != null && !coupon.getImageUrl().isEmpty()) {
                String newUrl = cloudinaryService.updateCouponImage(imageFile, coupon.getImageUrl());
                coupon.setImageUrl(newUrl);
            }
            // Nếu là thêm mới hoặc chưa có ảnh -> Upload mới
            else {
                String url = cloudinaryService.uploadCouponImage(imageFile);
                coupon.setImageUrl(url);
            }
        } else {
            // Nếu không chọn ảnh mới
            if (coupon.getId() != null) {
                // Lấy lại URL ảnh cũ từ Database (vì @ModelAttribute có thể làm mất giá trị này nếu form không gửi lên)
                Coupon oldCoupon = couponService.getById(coupon.getId());
                coupon.setImageUrl(oldCoupon.getImageUrl());
            }
        }
    }
}