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
        return customerRepository.findById(id).map(category -> {
            category.setName(updatedCustomer.getName());
            category.setCustomerSince(updatedCustomer.getCustomerSince());
            return customerRepository.save(category);
        });
    }
    public Optional<Customer> deleteCustomer(int id) {
        Optional<Customer> emp = customerRepository.findById(id);
        emp.ifPresent(customerRepository::delete);   // Nếu có thì xoá
        return emp;// Trả về Optional vừa xoá (nếu có)
    }
}
