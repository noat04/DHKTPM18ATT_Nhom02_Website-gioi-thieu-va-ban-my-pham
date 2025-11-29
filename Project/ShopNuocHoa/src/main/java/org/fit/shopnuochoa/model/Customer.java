package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import lombok.*;
import org.fit.shopnuochoa.Enum.Gender;

import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate customerSince;

    // [SỬA] Cho phép null (người dùng sẽ cập nhật sau)
    @Column(nullable = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = true) // [SỬA] Cho phép null
    private Gender gender;

    // [SỬA] Cho phép null
    @Column(nullable = true)
    private LocalDate birthday;

    // [SỬA] Cho phép null (để tránh lỗi khi đăng ký)
    @Column(name = "province")
    private String province; // Tỉnh/Thành phố (VD: Hà Nội)

    @Column(name = "district")
    private String district; // Quận/Huyện (VD: Quận Cầu Giấy)

    @Column(name = "ward")
    private String ward;     // Phường/Xã (VD: Phường Dịch Vọng)

    @Column(name = "street_detail")
    private String streetDetail; // Số nhà, tên đường (VD: 123 Xuân Thủy)

    // Hàm tiện ích để lấy địa chỉ đầy đủ hiển thị (khi in hóa đơn)
    @Transient
    public String getFullAddress() {
        if (streetDetail == null && province == null) return "Chưa cập nhật";
        return String.format("%s, %s, %s, %s", streetDetail, ward, district, province);
    }

    @Column(nullable = false)
    private String email;

    // [SỬA] Cho phép null
    @Column(nullable = true)
    private String idCard;

    // [SỬA] Cho phép null
    @Column(nullable = true)
    private String nickName;

    // Quan hệ 1-N: 1 Customer có nhiều Order
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Orders> orders;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Users user;
}