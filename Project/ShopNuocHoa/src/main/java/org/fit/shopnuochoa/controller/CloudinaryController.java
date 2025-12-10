package org.fit.shopnuochoa.controller;

import lombok.RequiredArgsConstructor;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.service.CloudinaryService;
import org.fit.shopnuochoa.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;
    private final UserService userService;

//    /**
//     * Xử lý xóa ảnh đại diện
//     * URL: POST /api/delete-avatar
//     */
//    @PostMapping("/delete-avatar")
//    public String deleteAvatar(Principal principal, RedirectAttributes redirectAttributes) {
//        try {
//            if (principal == null) {
//                return "redirect:/api/login";
//            }
//
//            String username = principal.getName();
//            Users currentUser = userService.getUserByUsername(username);
//
//            if (currentUser == null) {
//                throw new RuntimeException("User không tồn tại");
//            }
//
//            String currentAvatarUrl = currentUser.getAvatar();
//
//            // 1. Kiểm tra xem có phải ảnh mặc định không
//            if (currentAvatarUrl != null && currentAvatarUrl.startsWith("http")) {
//
//                // 2. Xóa trên Cloudinary (Nên bọc try-catch riêng để không chết app nếu lỗi mạng)
//                try {
//                    cloudinaryService.deleteImageByUrl(currentAvatarUrl);
//                } catch (Exception e) {
//                    System.err.println("Lỗi khi xóa ảnh trên Cloudinary (có thể bỏ qua): " + e.getMessage());
//                }
//
//                // 3. Xóa trong Database
//                currentUser.setAvatar(null);
//                userService.save(currentUser);
//
//                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ảnh đại diện!");
//            } else {
//                // Nếu avatar là null hoặc ảnh local
//                currentUser.setAvatar(null); // Đảm bảo về null
//                userService.save(currentUser);
//                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ảnh đại diện!");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa ảnh: " + e.getMessage());
//        }
//
//        return "redirect:/api/profile";
//    }
//
//    /**
//     * Xử lý upload ảnh đại diện mới
//     * URL: POST /api/upload-avatar
//     */
//    @PostMapping("/upload-avatar")
//    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file,
//                               Principal principal,
//                               RedirectAttributes redirectAttributes) {
//
//        if (principal == null) {
//            return "redirect:/api/login";
//        }
//
//        if (file.isEmpty()) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ảnh!");
//            return "redirect:/api/profile";
//        }
//
//        try {
//            String username = principal.getName();
//            Users currentUser = userService.getUserByUsername(username);
//
//            // 1. Lấy URL ảnh cũ
//            String oldAvatarUrl = currentUser.getAvatar();
//
//            // 2. Gọi hàm update (Nó sẽ tự xóa cũ -> up mới)
//            // Hàm updateAvatar trong CloudinaryService cần xử lý logic:
//            // Nếu oldAvatarUrl có tồn tại -> xóa nó -> sau đó upload file mới
//            String newAvatarUrl = cloudinaryService.updateAvatar(file, oldAvatarUrl);
//
//            // 3. Lưu URL mới vào DB
//            currentUser.setAvatar(newAvatarUrl);
//            userService.save(currentUser);
//
//            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật ảnh đại diện thành công!");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi upload: " + e.getMessage());
//        }
//
//        return "redirect:/api/profile";
//    }
}