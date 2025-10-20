package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.model.Comment;
import org.fit.shopnuochoa.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Hiển thị danh sách tất cả bình luận (dành cho admin) với phân trang.
     * Cũng xử lý hành động xóa.
     */
    @GetMapping("/list")
    public String showCommentList(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(value = "action", required = false) String action,
                                  @RequestParam(value = "id", required = false) Integer id,
                                  Model model) {
        // Xử lý xóa
        if ("delete".equals(action) && id != null) {
            commentService.deleteComment(id);
            return "redirect:/api/comments/list";
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentService.getAll(pageable);

        model.addAttribute("commentPage", commentPage);
        return "screen/comment-list"; // View để hiển thị danh sách comment
    }

    /**
     * Xử lý việc thêm bình luận mới từ trang chi tiết sản phẩm.
     */
    @PostMapping("/add")
    public String addComment(@RequestParam("productId") Integer productId,
                             @RequestParam("text") String text,
                             RedirectAttributes redirectAttributes) {

        if (text == null || text.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("commentError", "Vui lòng nhập nội dung bình luận.");
        } else {
            try {
                // Tạo một đối tượng Comment mới và gọi service để lưu
                Comment newComment = new Comment();
                newComment.setText(text);
                commentService.createComment(newComment, productId); // Gọi phương thức đã sửa tên
                redirectAttributes.addFlashAttribute("commentSuccess", "Cảm ơn bạn đã gửi bình luận!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("commentError", "Đã có lỗi xảy ra, không thể gửi bình luận.");
            }
        }

        // Chuyển hướng trở lại trang chi tiết sản phẩm
        return "redirect:/api/products/detail/" + productId;
    }
}