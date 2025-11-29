package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.repository.CustomerRepository;
import org.fit.shopnuochoa.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
    // [CẬP NHẬT QUAN TRỌNG] Hàm Update hỗ trợ tách địa chỉ
    public Optional<Customer> updateCustomer(int id, Customer updatedCustomer) {
        return customerRepository.findById(id).map(existingCustomer -> {

            // 1. Cập nhật thông tin cơ bản
            if (updatedCustomer.getName() != null && !updatedCustomer.getName().trim().isEmpty()) {
                existingCustomer.setName(updatedCustomer.getName());
            }
            if (updatedCustomer.getPhoneNumber() != null && !updatedCustomer.getPhoneNumber().trim().isEmpty()) {
                existingCustomer.setPhoneNumber(updatedCustomer.getPhoneNumber());
            }
            if (updatedCustomer.getGender() != null) {
                existingCustomer.setGender(updatedCustomer.getGender());
            }
            if (updatedCustomer.getBirthday() != null) {
                existingCustomer.setBirthday(updatedCustomer.getBirthday());
            }
            if (updatedCustomer.getIdCard() != null && !updatedCustomer.getIdCard().trim().isEmpty()) {
                existingCustomer.setIdCard(updatedCustomer.getIdCard());
            }
            if (updatedCustomer.getNickName() != null && !updatedCustomer.getNickName().trim().isEmpty()) {
                existingCustomer.setNickName(updatedCustomer.getNickName());
            }
            if (updatedCustomer.getEmail() != null && !updatedCustomer.getEmail().trim().isEmpty()) {
                existingCustomer.setEmail(updatedCustomer.getEmail());
            }

            // 2. [MỚI] Cập nhật Địa chỉ chi tiết (Tách riêng)
            // Chỉ cập nhật nếu người dùng có chọn/nhập giá trị mới

            if (updatedCustomer.getProvince() != null && !updatedCustomer.getProvince().isEmpty()) {
                existingCustomer.setProvince(updatedCustomer.getProvince());
            }

            if (updatedCustomer.getDistrict() != null && !updatedCustomer.getDistrict().isEmpty()) {
                existingCustomer.setDistrict(updatedCustomer.getDistrict());
            }

            if (updatedCustomer.getWard() != null && !updatedCustomer.getWard().isEmpty()) {
                existingCustomer.setWard(updatedCustomer.getWard());
            }

            if (updatedCustomer.getStreetDetail() != null && !updatedCustomer.getStreetDetail().isEmpty()) {
                existingCustomer.setStreetDetail(updatedCustomer.getStreetDetail());
            }

            // (Trường 'address' cũ không còn dùng để set nữa, nó được thay thế bằng getFullAddress() khi hiển thị)

            return customerRepository.save(existingCustomer);
        });
    }
    public Optional<Customer> deleteCustomer(int id) {
        Optional<Customer> emp = customerRepository.findById(id);
        emp.ifPresent(customerRepository::delete);   // Nếu có thì xoá
        return emp;// Trả về Optional vừa xoá (nếu có)
    }
}
