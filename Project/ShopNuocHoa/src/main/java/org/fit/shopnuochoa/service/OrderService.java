package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.Enum.OrderStatus;
import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Orders;
import org.fit.shopnuochoa.repository.CustomerRepository;
import org.fit.shopnuochoa.repository.OrdersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<Orders> getByCustomer(Integer customerId) {return ordersRepository.findByCustomerId(customerId);}
    // ✅ Thêm hàm này
    public Page<Orders> findAll(Pageable pageable) {
        return ordersRepository.findAll(pageable);
    }
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

    @Transactional
    public void confirmReceived(Integer orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));

        // Chỉ cho phép xác nhận khi đơn đang ở trạng thái DELIVERED (Đã giao)
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Đơn hàng chưa được giao xong hoặc trạng thái không hợp lệ.");
        }

        // Cập nhật trạng thái thành công
        order.setStatus(OrderStatus.COMPLETED);
        ordersRepository.save(order);
    }
    public Page<Orders> findByDate(LocalDate currentDate, Pageable pageable) {
        return ordersRepository.findByDate(currentDate, pageable);
    }
    public Page<Orders> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return ordersRepository.findByDateRange(startDate.atStartOfDay(), endDate.atStartOfDay(),pageable);
    }

    public Page<Orders> searchByCustomerNameOrUsername(String keyword, Pageable pageable) {
        return ordersRepository.searchByCustomerNameOrUsername(keyword, pageable);
    }

    public long countByCustomerId(Integer customerId) {
        return ordersRepository.countByCustomerId(customerId);
    }
    public long countOrdersInWeek() {
        return ordersRepository.countOrdersInCurrentWeek();
    }

    public long countOrdersInMonth() {
        return ordersRepository.countOrdersInCurrentMonth();
    }
}
