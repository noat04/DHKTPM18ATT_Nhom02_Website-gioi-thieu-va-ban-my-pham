package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Role;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public List<Users> getAll() {
        return userRepository.findAll();
    }
    public Users getUserById(int id) {
        return userRepository.findById(id).orElse(null);
    }
    public Users getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public Users createUser(Users user) {
        // Băm mật khẩu trước khi lưu
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);

        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    public Optional<Users> updateUser(int id, Users updatedUsers) {
        return userRepository.findById(id).map(user -> {

            // ✅ Cập nhật các thông tin cơ bản
            user.setFull_name(updatedUsers.getFull_name());
            user.setEmail(updatedUsers.getEmail());
            user.setRole(updatedUsers.getRole());
            user.setActive(updatedUsers.isActive());

            // ✅ Nếu có mật khẩu mới thì mã hóa và cập nhật
            if (updatedUsers.getPassword() != null && !updatedUsers.getPassword().isEmpty()) {
                String hashedPassword = passwordEncoder.encode(updatedUsers.getPassword());
                user.setPassword(hashedPassword);
            }

            // ❌ Không ghi đè username vì field này readonly
            return userRepository.save(user);
        });
    }

    public Users registerNewUser(Users user) {
        // 1. Kiểm tra username đã tồn tại chưa
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        // 2. Kiểm tra email đã tồn tại chưa
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }
        // 3. Băm mật khẩu
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 4. Thiết lập vai trò mặc định là CUSTOMER
        user.setRole(Role.CUSTOMER);

        // 5. Kích hoạt tài khoản
        user.setActive(true);

        return userRepository.save(user);
    }
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    public boolean deleteUserById(int id) {
        return userRepository.findById(id).map(user -> {
            // Nếu user có liên kết tới Customer và Customer có Orders -> không xóa
            if (user.getCustomer() != null &&
                    user.getCustomer().getOrders() != null &&
                    !user.getCustomer().getOrders().isEmpty()) {
                throw new IllegalStateException("Không thể xóa tài khoản đã từng đặt hàng.");
            }
            userRepository.delete(user);
            return true;
        }).orElse(false);
    }
}
