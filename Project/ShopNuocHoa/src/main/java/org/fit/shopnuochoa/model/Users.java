package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 100)
    private String full_name;

    // Tên trường trong Java là 'password', nhưng map tới cột 'password_hash'
    // Luôn lưu trữ mật khẩu đã được băm (hashed)
    @Column(name = "password_hash", nullable = false)
    @ToString.Exclude // Không bao giờ đưa mật khẩu vào phương thức toString()
    private String password;

    // Sử dụng EnumType.STRING để lưu tên của vai trò ("ADMIN", "CUSTOMER") vào CSDL
    // Cách này an toàn và dễ đọc hơn EnumType.ORDINAL (lưu số 0, 1, 2)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // Tự động gán ngày giờ hiện tại khi tạo mới
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Quan hệ 1-1: User là phía "không sở hữu" (inverse side)
    // Mối quan hệ này được quản lý bởi thuộc tính "user" trong class Customer
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude // Tránh vòng lặp vô hạn khi gọi toString()
    private Customer customer;

}