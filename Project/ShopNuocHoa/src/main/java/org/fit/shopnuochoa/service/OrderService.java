package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.repository.CustomerRepository;
import org.fit.shopnuochoa.repository.OrdersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private OrdersRepository ordersRepository;
    private CustomerRepository customerRepository;
    public OrderService(OrdersRepository ordersRepository,CustomerRepository customerRepository) {
        this.ordersRepository = ordersRepository;
        this.customerRepository = customerRepository;
    }
    public List<Orders> findAll() {return ordersRepository.findAll();}
    public Optional<Orders> findById(int id) {
        return ordersRepository.findById(id);
    }
    public Orders createOrder(Orders orders) {
//        Customer customer = customerRepository.findById(customerId).orElse(null);
//        orders.setCustomer(customer);
        return ordersRepository.save(orders);
    }
    public Optional<Orders> updateOrder(int id, Orders orderUpdate, Integer customerId) {
        return ordersRepository.findById(id).map(orders -> {
            orders.setDate(orderUpdate.getDate());
            // Cập nhật Category một cách an toàn
            if (customerId != null) {
                Customer category = customerRepository.findById(customerId)
                        .orElseThrow(() -> new RuntimeException("Category not found with id: " + customerId));
                orders.setCustomer(category);
            }
            return ordersRepository.save(orders);
        });
    }
    public Optional<Orders> deleteOrders(int id) {
        Optional<Orders> emp = ordersRepository.findById(id);
        emp.ifPresent(ordersRepository::delete);   // Nếu có thì xoá
        return emp;// Trả về Optional vừa xoá (nếu có)
    }
    public Page<Orders> findByCustomer(int id, Pageable pageable) {
        return ordersRepository.findByCustomerId(id, pageable);
    }
    public Page<Orders> findByDate(LocalDate currentDate, Pageable pageable) {
        return ordersRepository.findByDate(currentDate, pageable);
    }
    public Page<Orders> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return ordersRepository.findByDateRange(startDate.atStartOfDay(), endDate.atStartOfDay(),pageable);
    }

}
