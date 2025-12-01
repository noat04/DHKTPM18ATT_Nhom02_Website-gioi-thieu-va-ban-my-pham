package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Comment;
import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.repository.CommentRepository;
import org.fit.shopnuochoa.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
@Service
public class CommentService {
    private CommentRepository commentRepository;
    private ProductService productService;
    private CustomerService customerService;

    public CommentService(CommentRepository commentRepository, ProductService productService, CustomerService customerService) {
        this.commentRepository = commentRepository;
        this.productService = productService;
        this.customerService = customerService; // <-- Gán service
    }

    public Page<Comment> getAll(Pageable pageable) {return commentRepository.findAll(pageable);}

    public Comment getById(Integer id) {return commentRepository.findById(id).orElse(null);}

    public Page<Comment> getByProductId(Integer id, Pageable pageable) {return commentRepository.findByProductId(id, pageable);}


    @Transactional
    public Comment createComment(Comment comment, Integer productId, Integer customerId) {
        // 1. Tìm Product
        Product product = productService.getById(productId);
        if (product == null) {
            throw new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId);
        }

        // 2. Tìm Customer
        Customer customer = customerService.getById(customerId); // (Giả sử bạn có hàm này)
        if (customer == null) {
            throw new RuntimeException("Không tìm thấy khách hàng với ID: " + customerId);
        }

        // 3. Gán cả hai vào comment
        comment.setProduct(product);
        comment.setCustomer(customer); // <-- Gán khách hàng

        // 4. Lưu comment
        Comment savedComment = commentRepository.save(comment);

        // 5. Kích hoạt cập nhật rating (Giữ nguyên)
        productService.updateRatingStats(productId);
        return savedComment;
    }

    // Cập nhật
    public Optional<Comment> updateAdminComment(int id, Comment updatedComment) {
        return commentRepository.findById(id).map(category -> {
            category.setText(updatedComment.getText());
            return commentRepository.save(category);
        });
    }

    // Cập nhật (cho Admin - Bạn có thể giữ lại nếu admin cần)
    public Optional<Comment> updateComment(int id, Comment updatedComment) {
        return commentRepository.findById(id).map(comment -> {
            comment.setText(updatedComment.getText());
            return commentRepository.save(comment);
        });
    }

    /**
     * [THÊM MỚI]
     * Cho phép người dùng tự cập nhật bình luận của chính họ.
     */
    @Transactional
    public Comment updateUserComment(Integer commentId, String newText, Integer customerId) {
        // 1. Tìm bình luận
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận."));

        // 2. Kiểm tra quyền sở hữu
        if (comment.getCustomer() == null || !comment.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("Bạn không có quyền sửa bình luận này.");
        }

        // 3. Cập nhật và lưu
        comment.setText(newText);
        return commentRepository.save(comment);
    }

    @Transactional // Thêm Transactional
    public Optional<Comment> deleteComment(int id) {
        Optional<Comment> optComment = commentRepository.findById(id);
        if (optComment.isPresent()) {
            Comment comment = optComment.get();
            Integer productId = comment.getProduct().getId(); // Lấy productId TRƯỚC khi xóa
            commentRepository.delete(comment); // 1. Xóa comment
            // 2. KÍCH HOẠT CẬP NHẬT RATING
            productService.updateRatingStats(productId);
        }
        return optComment;
    }
}
