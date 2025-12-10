package org.fit.shopnuochoa.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*; // Import thư viện Validate
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

    // Tên bắt buộc phải có
    @Column(nullable = false)
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    private String name;

    @Column(nullable = false)
    private LocalDate customerSince;

    //Số điện thoại: Không bắt buộc (@NotNull), nhưng nếu nhập thì phải đúng định dạng VN
    @Column(nullable = true)
    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$",
            message = "Số điện thoại không hợp lệ (VD: 0912345678)")
    private String phoneNumber;

    //Giới tính: Optional
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = true)
    private Gender gender;

    // Ngày sinh: Optional, nhưng nếu nhập thì phải là ngày trong quá khứ
    @Column(nullable = true)
    @Past(message = "Ngày sinh không hợp lệ (phải là ngày trong quá khứ)")
    private LocalDate birthday;

    // Địa chỉ: Optional, kiểm tra độ dài để tránh lỗi DB
    @Column(name = "province")
    @Size(max = 100, message = "Tên tỉnh/thành quá dài")
    private String province;

    @Column(name = "district")
    @Size(max = 100, message = "Tên quận/huyện quá dài")
    private String district;

    @Column(name = "ward")
    @Size(max = 100, message = "Tên phường/xã quá dài")
    private String ward;

    @Column(name = "street_detail")
    @Size(max = 255, message = "Địa chỉ chi tiết quá dài")
    private String streetDetail;

    // Hàm tiện ích để lấy địa chỉ đầy đủ hiển thị (khi in hóa đơn)
    @Transient
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();

        if (streetDetail != null) sb.append(streetDetail);
        if (ward != null) sb.append(", ").append(ward);
        if (district != null) sb.append(", ").append(district);
        if (province != null) sb.append(", ").append(province);

        return sb.length() > 0 ? sb.toString() : "Chưa cập nhật";
    }



    //Email: Bắt buộc và phải đúng định dạng
    private String email;

    //CCCD: Optional, nhưng nếu nhập phải đủ độ dài
    @Column(nullable = true)
//    @Pattern(regexp = "^\\d{9}|\\d{12}$", message = "CCCD/CMND phải là 9 hoặc 12 chữ số")
    private String idCard;

    //Nickname: Optional
    @Column(nullable = true)
    @Size(max = 50, message = "Biệt danh tối đa 50 ký tự")
    private String nickName;

    // Quan hệ 1-N
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Orders> orders;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Users user;
}