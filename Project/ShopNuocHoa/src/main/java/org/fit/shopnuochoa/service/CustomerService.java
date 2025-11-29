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
    public Optional<Customer> updateCustomer(int id, Customer updatedCustomer) {
        return customerRepository.findById(id).map(existingCustomer -> {

            // 1. Cập nhật Tên (Nếu có)
            if (updatedCustomer.getName() != null && !updatedCustomer.getName().trim().isEmpty()) {
                existingCustomer.setName(updatedCustomer.getName());
            }

            // 2. Cập nhật Số điện thoại (Nếu có)
            if (updatedCustomer.getPhoneNumber() != null && !updatedCustomer.getPhoneNumber().trim().isEmpty()) {
                existingCustomer.setPhoneNumber(updatedCustomer.getPhoneNumber());
            }

            // 3. Cập nhật Địa chỉ (Nếu có)
            if (updatedCustomer.getAddress() != null && !updatedCustomer.getAddress().trim().isEmpty()) {
                existingCustomer.setAddress(updatedCustomer.getAddress());
            }

            // 4. Cập nhật Giới tính (Nếu có)
            if (updatedCustomer.getGender() != null) {
                existingCustomer.setGender(updatedCustomer.getGender());
            }

            // 5. Cập nhật Ngày sinh (Nếu có)
            if (updatedCustomer.getBirthday() != null) {
                existingCustomer.setBirthday(updatedCustomer.getBirthday());
            }

            // 6. Cập nhật Email (Nếu có - cẩn thận vì email thường là định danh)
            if (updatedCustomer.getEmail() != null && !updatedCustomer.getEmail().trim().isEmpty()) {
                existingCustomer.setEmail(updatedCustomer.getEmail());
            }

            // 7. Cập nhật CMND/CCCD (Nếu có)
            if (updatedCustomer.getIdCard() != null && !updatedCustomer.getIdCard().trim().isEmpty()) {
                existingCustomer.setIdCard(updatedCustomer.getIdCard());
            }

            // 8. Cập nhật Nickname (Nếu có)
            if (updatedCustomer.getNickName() != null && !updatedCustomer.getNickName().trim().isEmpty()) {
                existingCustomer.setNickName(updatedCustomer.getNickName());
            }

            // [QUAN TRỌNG] KHÔNG cập nhật 'customerSince' (ngày tham gia)
            // vì nó là thông tin lịch sử không đổi.

            return customerRepository.save(existingCustomer);
        });
    }
    public Optional<Customer> deleteCustomer(int id) {
        Optional<Customer> emp = customerRepository.findById(id);
        emp.ifPresent(customerRepository::delete);   // Nếu có thì xoá
        return emp;// Trả về Optional vừa xoá (nếu có)
    }
}
