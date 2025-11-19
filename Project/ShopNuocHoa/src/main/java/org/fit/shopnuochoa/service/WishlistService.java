package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Customer; // <-- Thêm import
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.model.Wishlist;
import org.fit.shopnuochoa.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductService productService;
    private final UserService userService;
    private final CustomerService customerService; // <-- Thêm CustomerService

    @Autowired
    public WishlistService(WishlistRepository wishlistRepository,
                           ProductService productService,
                           UserService userService,
                           CustomerService customerService) { // <-- Cập nhật constructor
        this.wishlistRepository = wishlistRepository;
        this.productService = productService;
        this.userService = userService;
        this.customerService = customerService; // <-- Gán service
    }

    // [SỬA ĐỔI] Nhận customerId
    public boolean existsInWishlist(Integer customerId, Integer productId) {
        return wishlistRepository.existsByCustomerIdAndProductId(customerId, productId);
    }

    // [SỬA ĐỔI] Nhận customerId và dùng CustomerService
    public void toggleWishlist(int customerId, int productId) {
        Optional<Wishlist> existing = wishlistRepository.findByCustomerIdAndProductId(customerId, productId);

        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
        } else {
            Wishlist w = new Wishlist();

            // Lấy thực thể Customer và Product
            // (Giả sử bạn có hàm getCustomerById() trong CustomerService)
            Customer customer = customerService.getById(customerId);
            Product product = productService.getById(productId);

            if (customer == null || product == null) {
                throw new RuntimeException("Không tìm thấy Customer hoặc Product");
            }

            w.setCustomer(customer); // <-- Gán Customer
            w.setProduct(product);
            wishlistRepository.save(w);
        }
    }

    // [SỬA ĐỔI] Đổi tên và logic để trả về Customer ID
    public Integer getCustomerIdByUsername(String username) {
        Users user = userService.getUserByUsername(username);
        // Lấy Customer ID từ User (logic tương tự CommentController)
        if (user != null && user.getCustomer() != null) {
            return user.getCustomer().getId();
        }
        return null;
    }
}