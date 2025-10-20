package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Comment;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.repository.CommentRepository;
import org.fit.shopnuochoa.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public class CommentService {
    private CommentRepository commentRepository;
    private ProductRepository productRepository;
    public CommentService(CommentRepository commentRepository, ProductRepository productRepository) {
        this.commentRepository = commentRepository;
        this.productRepository = productRepository;
    }
    public Page<Comment> getAll(Pageable pageable) {return commentRepository.findAll(pageable);}
    public Comment getById(Integer id) {return commentRepository.findById(id).orElse(null);}
    public Page<Comment> getByProductId(Integer id, Pageable pageable) {return commentRepository.findByProductId(id, pageable);}
    public Comment createComment(Comment comment, Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        comment.setProduct(product);
        return commentRepository.save(comment);
    }
    // Cập nhật
    public Optional<Comment> updateComment(int id, Comment updatedComment) {
        return commentRepository.findById(id).map(category -> {
            category.setText(updatedComment.getText());
            return commentRepository.save(category);
        });
    }
    public Optional<Comment> deleteComment(int id) {
        Optional<Comment> emp = commentRepository.findById(id);
        emp.ifPresent(commentRepository::delete);   // Nếu có thì xoá
        return emp;// Trả về Optional vừa xoá (nếu có)
    }
}
