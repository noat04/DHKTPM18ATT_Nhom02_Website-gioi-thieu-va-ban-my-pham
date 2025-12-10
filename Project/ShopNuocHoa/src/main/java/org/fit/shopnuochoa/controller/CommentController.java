package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.model.Comment;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.service.CommentService;
import org.fit.shopnuochoa.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/api/comments")
public class CommentController {
    private final UserService userService;
    private final CommentService commentService;

    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    /**
     * Hiển thị danh sách tất cả bình luận (dành cho admin) với phân trang.
     * xử lý hành động xóa.
     */
    @GetMapping("/list")
    public String showCommentList(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(value = "action", required = false) String action,
                                  @RequestParam(value = "id", required = false) Integer id,
                                  Model model) {

        // Xử lý xóa và chuyển hướng về trang sản phẩm
        if ("delete".equals(action) && id != null) {
            // 1. Gọi hàm xóa (hàm này trả về Optional<Comment> đã xóa)
            Optional<Comment> deletedCommentOpt = commentService.deleteComment(id);

            // 2. Nếu xóa thành công, lấy Product ID và chuyển hướng
            if (deletedCommentOpt.isPresent()) {
                Integer productId = deletedCommentOpt.get().getProduct().getId();
                return "redirect:/api/products/detail/" + productId;
            }

            // 3. Nếu không tìm thấy comment (lỗi), quay về danh sách chung
            return "redirect:/api/comments/list";
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentService.getAll(pageable);
        model.addAttribute("commentPage", commentPage);
        return "screen/customer/product-detail";
    }

    /**
     * Xử lý việc thêm bình luận mới.
     * 1. Thêm Authentication để biết ai đang đăng nhập.
     * 2. Thêm @RequestParam("rating") để lấy số sao.
     * 3. Tìm customerId từ Authentication.
     * 4. Gọi service với đầy đủ 3 tham số.
     */
    @PostMapping("/add")
    public String addComment(@RequestParam("productId") Integer productId,
                             @RequestParam("text") String text,
                             @RequestParam("rating") int rating,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra xem người dùng đã đăng nhập chưa
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("commentError", "Vui lòng đăng nhập để bình luận.");
            return "redirect:/api/products/detail/" + productId;
        }

        // 2. Kiểm tra nội dung
        if (text == null || text.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("commentError", "Vui lòng nhập nội dung bình luận.");
        } else {
            try {
                // 3. Lấy customerId từ user đang đăng nhập
                String username = authentication.getName();
                Users user = userService.getUserByUsername(username); // (Cần có hàm này trong UserService)

                if (user == null || user.getCustomer() == null) {
                    throw new RuntimeException("Không tìm thấy thông tin khách hàng cho user: " + username);
                }
                Integer customerId = user.getCustomer().getId();

                // 4. Tạo và lưu bình luận
                Comment newComment = new Comment();
                newComment.setText(text);
                newComment.setRating(rating); // <-- Gán rating

                // Gọi hàm createComment đã sửa (với 3 tham số)
                commentService.createComment(newComment, productId, customerId);

                redirectAttributes.addFlashAttribute("commentSuccess", "Cảm ơn bạn đã gửi bình luận!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("commentError", "Đã có lỗi xảy ra: " + e.getMessage());
            }
        }

        // Chuyển hướng trở lại trang chi tiết sản phẩm
        return "redirect:/api/products/detail/" + productId;
    }

    /**
     * Xử lý yêu cầu SỬA bình luận (từ AJAX).
     * Trả về JSON (ResponseEntity)
     */
    @PostMapping("/edit")
    public ResponseEntity<?> editComment(@RequestParam("commentId") Integer commentId,
                                         @RequestParam("text") String newText,
                                         Authentication authentication) {

        // 1. Kiểm tra đăng nhập
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập.");
        }

        try {
            // 2. Lấy Customer ID
            String username = authentication.getName();
            Users user = userService.getUserByUsername(username);
            if (user == null || user.getCustomer() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy thông tin khách hàng.");
            }
            Integer customerId = user.getCustomer().getId();

            // 3. Gọi service để cập nhật
            Comment updatedComment = commentService.updateUserComment(commentId, newText, customerId);

            // 4. Trả về thành công
            return ResponseEntity.ok(updatedComment); // Trả về comment đã cập nhật (JS có thể dùng)

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Hiển thị danh sách bình luận cho ADMIN
     */
    @GetMapping("/admin")
    public String showAdminCommentList(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(value = "action", required = false) String action,
                                       @RequestParam(value = "id", required = false) Integer id,
                                       Model model) {

        // Xử lý xóa (cho phía admin)
        if ("delete".equals(action) && id != null) {
            commentService.deleteComment(id);
            return "redirect:/api/comments/admin";
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentService.getAll(pageable);

        model.addAttribute("commentPage", commentPage);

        // Trả về file admin-comment.html
        return "screen/admin/admin-comment";
    }
}