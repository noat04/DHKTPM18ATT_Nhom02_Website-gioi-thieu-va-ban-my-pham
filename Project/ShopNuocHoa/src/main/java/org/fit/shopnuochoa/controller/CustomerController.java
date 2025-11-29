package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/customers")
public class CustomerController {

    private CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/list")
//    @PreAuthorize("hasAnyRole('ADMIN')")
    public String showCategoryList(@RequestParam(value = "action", required = false) String action,
                                   @RequestParam(value = "id", required = false) Integer id,
                                   @RequestParam(defaultValue = "0") int page,      // Tham số cho trang hiện tại
                                   @RequestParam(defaultValue = "4") int size,
                                   Model model) {
        Pageable pageable = PageRequest.of(page, size);
        // Xử lý hành động xóa nếu có
        if ("delete".equals(action) && id != null) {
            customerService.deleteCustomer(id); // Gọi service để xóa
            return "redirect:/api/customers/list"; // Chuyển hướng để làm mới danh sách
        }

        // Lấy danh sách tất cả phòng ban để hiển thị
        Page<Customer> customers = customerService.getAll(pageable);
        model.addAttribute("customers", customers);
        return "screen/admin/admin-customer-list"; // Trả về file view department-list.html
    }

    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public String showDepartmentForm(@RequestParam(value = "action", required = false, defaultValue = "add") String action,
                                     @RequestParam(value = "id", required = false) Integer id,
                                     Model model) {
        Customer customer;

        if ("edit".equals(action) && id != null) {
            // Nếu là chỉnh sửa, lấy thông tin phòng ban từ DB
            customer= customerService.getById(id);
        } else {
            // Nếu là thêm mới, tạo một đối tượng rỗng
            customer= new Customer();
        }

        model.addAttribute("customer", customer);
        model.addAttribute("action", action); // Truyền action để form biết là 'add' hay 'edit'
        return "screen/customer-form"; // Trả về file view department-form.html
    }

    /**
     * Xử lý dữ liệu được gửi từ form (thêm mới hoặc cập nhật).
     */
    @PostMapping("/form")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public String handleDepartmentForm(@RequestParam("action") String action,
                                       Customer customer) { // Spring tự động binding dữ liệu từ form vào đối tượng

        if ("add".equals(action)) {
            customerService.createCustomerFromAdmin(customer);
        } else if ("edit".equals(action)) {
            customerService.updateCustomer(customer.getId(), customer);
        }

        return "redirect:/api/customers/list"; // Chuyển hướng về trang danh sách sau khi xử lý
    }
}
