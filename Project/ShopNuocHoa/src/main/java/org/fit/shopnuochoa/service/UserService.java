package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Role;
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
    public Optional<Users> updateUser(int id, Users updatedUsers) {
        return userRepository.findById(id).map(user -> {
            // Chỉ băm và cập nhật mật khẩu nếu có mật khẩu mới được cung cấp
            if (updatedUsers.getPassword() != null && !updatedUsers.getPassword().isEmpty()) {
                String hashedPassword = passwordEncoder.encode(updatedUsers.getPassword());
                user.setPassword(hashedPassword);
            }

            user.setUsername(updatedUsers.getUsername());
            user.setPassword(updatedUsers.getPassword());
            user.setEmail(updatedUsers.getEmail());
            user.setFull_name(updatedUsers.getFull_name());
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

}
