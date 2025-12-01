package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.Enum.Role;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.repository.UserRepository;
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

    public Optional<Users> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Hàm này dùng để lưu lại thông tin User đã chỉnh sửa.
     * Vì User đã có ID nên JPA sẽ tự hiểu là UPDATE.
     */
    public Users save(Users user) {
        return userRepository.save(user);
    }

    /**
     * (Tùy chọn) Cập nhật thêm trường Avatar vào hàm updateUser cũ nếu cần
     */
    public Optional<Users> updateUser(int id, Users updatedUsers) {
        return userRepository.findById(id).map(user -> {
            user.setFull_name(updatedUsers.getFull_name());
            user.setEmail(updatedUsers.getEmail());
            user.setRole(updatedUsers.getRole());
            user.setActive(updatedUsers.isActive());

            // ✅ BỔ SUNG: Cập nhật avatar nếu có
            if (updatedUsers.getAvatar() != null) {
                user.setAvatar(updatedUsers.getAvatar());
            }

            if (updatedUsers.getPassword() != null && !updatedUsers.getPassword().isEmpty()) {
                String hashedPassword = passwordEncoder.encode(updatedUsers.getPassword());
                user.setPassword(hashedPassword);
            }
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

    public void updatePassword(String email, String newPassword) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email này"));

        // Mã hóa mật khẩu mới trước khi lưu
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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

    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    public long count() {
        return userRepository.count();
    }
}
