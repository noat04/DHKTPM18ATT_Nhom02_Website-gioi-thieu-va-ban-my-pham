package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
// Thêm các import cho Validation
import jakarta.validation.constraints.*;
import lombok.*;
import org.fit.shopnuochoa.Enum.Role;
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

    // Validate Username
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._-]*$", message = "Tên đăng nhập không được chứa ký tự đặc biệt")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // Validate Email
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không được quá 100 ký tự")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Validate Full Name
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2 đến 100 ký tự")
    @Column(nullable = false, unique = true, length = 100)
    private String full_name;

    @Column(name = "avatar")
    private String avatar;

    @Transient
    public String getAvatarPath() {
        if (avatar == null || avatar.isEmpty()) {
            return "/images/default-user.jpg";
        }
        if (avatar.startsWith("http")) {
            return avatar;
        }
        return "/user-photos/" + id + "/" + avatar;
    }

    // Validate Password
    // Lưu ý: Vì đây là Entity lưu vào DB (lưu mã hash), nên @Size ở đây kiểm tra độ dài chuỗi hash.
    // Việc kiểm tra độ mạnh mật khẩu (ví dụ: phải có chữ hoa, số...) nên làm ở DTO (RegisterForm).
    @NotBlank(message = "Mật khẩu không được để trống")
    @Column(name = "password_hash", nullable = false)
    @ToString.Exclude
    private String password;

    @NotNull(message = "Vai trò không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Customer customer;

}