package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.repository.CustomerRepository;
import org.fit.shopnuochoa.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class CustomerService {
    private CustomerRepository customerRepository;
    private UserRepository userRepository;
    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }
    public Page<Customer> getAll(Pageable pageable) {return customerRepository.findAll(pageable);}
    public Customer getByUser(int id) {return customerRepository.findByUserId(id);}
    public Customer getById(Integer id) {return customerRepository.findById(id).orElse(null);}
    public Customer createCustomer(Customer customer, Integer userId) {
        Users user = userRepository.findById(userId).orElse(null);
        customer.setName(user.getFull_name());
        customer.setEmail(user.getEmail());
        customer.setUser(user);
        customer.setCustomerSince(LocalDate.now());
        return customerRepository.save(customer);
    }

    public Customer createCustomerFromAdmin(Customer customer) {
        // Trường hợp admin thêm mới khách hàng, không có userId
        customer.setCustomerSince(LocalDate.now());
        return customerRepository.save(customer);
    }
    // Cập nhật
    @Transactional // Đảm bảo tính toàn vẹn dữ liệu khi update
    public Optional<Customer> updateCustomer(int id, Customer updatedCustomer) {
        return customerRepository.findById(id).map(existingCustomer -> {

            // 1. Cập nhật thông tin cá nhân cơ bản
            if (updatedCustomer.getName() != null && !updatedCustomer.getName().trim().isEmpty()) {
                existingCustomer.setName(updatedCustomer.getName());
                System.out.println("-> Tên");
            }
            if (updatedCustomer.getPhoneNumber() != null && !updatedCustomer.getPhoneNumber().trim().isEmpty()) {
                existingCustomer.setPhoneNumber(updatedCustomer.getPhoneNumber());
                System.out.println("-> số điện thoại");
            }
            if (updatedCustomer.getGender() != null) {
                existingCustomer.setGender(updatedCustomer.getGender());
                System.out.println("-> giới tính");
            }
            if (updatedCustomer.getBirthday() != null) {
                existingCustomer.setBirthday(updatedCustomer.getBirthday());
                System.out.println("-> sinh nhật");
            }
            if (updatedCustomer.getIdCard() != null && !updatedCustomer.getIdCard().trim().isEmpty()) {
                existingCustomer.setIdCard(updatedCustomer.getIdCard());
                System.out.println("-> cccd");
            }
            if (updatedCustomer.getNickName() != null && !updatedCustomer.getNickName().trim().isEmpty()) {
                existingCustomer.setNickName(updatedCustomer.getNickName());
                System.out.println("-> tên gọi");
            }
            // Email thường ít khi cho đổi, nhưng nếu cần thì giữ lại
//            if (updatedCustomer.getEmail() != null && !updatedCustomer.getEmail().trim().isEmpty()) {
//                existingCustomer.setEmail(updatedCustomer.getEmail());
//                System.out.println("-> email");
//            }

            // 2. [CẬP NHẬT ĐỊA CHỈ MỚI - 4 TRƯỜNG]
            // Kiểm tra null và rỗng để tránh ghi đè dữ liệu cũ bằng dữ liệu rỗng

            if (updatedCustomer.getProvince() != null && !updatedCustomer.getProvince().isBlank()) {
                existingCustomer.setProvince(updatedCustomer.getProvince());
                System.out.println("-> Đã set Tỉnh");
            }

            if (updatedCustomer.getDistrict() != null && !updatedCustomer.getDistrict().isBlank()) {
                existingCustomer.setDistrict(updatedCustomer.getDistrict());
                System.out.println("-> Đã set Huyện");
            }

            if (updatedCustomer.getWard() != null && !updatedCustomer.getWard().isBlank()) {
                existingCustomer.setWard(updatedCustomer.getWard());
                System.out.println("-> Đã set Xã");
            }

            if (updatedCustomer.getStreetDetail() != null && !updatedCustomer.getStreetDetail().isBlank()) {
                existingCustomer.setStreetDetail(updatedCustomer.getStreetDetail());
                System.out.println("-> Đã set Đường");
            }

            // LƯU Ý: Ta KHÔNG cập nhật trường 'address' cũ nữa.
            // Khi hiển thị, ta sẽ dùng hàm getFullAddress() trong Model để ghép 4 trường trên lại.

            return customerRepository.saveAndFlush(existingCustomer);
        });
    }
    public Optional<Customer> deleteCustomer(int id) {
        Optional<Customer> emp = customerRepository.findById(id);
        emp.ifPresent(customerRepository::delete);   // Nếu có thì xoá
        return emp;// Trả về Optional vừa xoá (nếu có)
    }
}
