package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.Enum.OrderStatus;
import org.fit.shopnuochoa.Enum.PaymentMethod;
import org.fit.shopnuochoa.Enum.ShippingMethod;
import org.fit.shopnuochoa.model.*;
import org.fit.shopnuochoa.repository.OrderLineRepository;
import org.fit.shopnuochoa.repository.OrdersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

@Service
public class CheckOutService {
    private final ProductService productService;
    private final OrderService orderService;
    private final OrdersRepository ordersRepository;
    private final CustomerService customerService;
    private final OrderLineRepository orderLineRepository;
    private final CouponService couponService;

    public CheckOutService(ProductService productService,
                           OrderService orderService,
                           CustomerService customerService,
                           OrderLineRepository orderLineRepository,
                           OrdersRepository ordersRepository,
                           CouponService couponService) {
        this.productService = productService;
        this.orderService = orderService;
        this.customerService = customerService;
        this.orderLineRepository = orderLineRepository;
        this.ordersRepository=ordersRepository;
        this.couponService=couponService;
    }

    /**
     * Dùng để kiểm tra giỏ hàng trước khi cho phép người dùng đến trang xác nhận.
     *  kiểm tra số lượng tồn kho (quantity)
     */
    public List<String> validateCart(CartBean cart) {
        List<String> errors = new ArrayList<>();
        if (cart == null || cart.getItems().isEmpty()) {
            errors.add("Giỏ hàng đang trống.");
            return errors;
        }
        for (CartItemBean item : cart.getItems()) {
            Product productInDb = productService.getById(item.getProduct().getId());

            if (productInDb == null) {
                errors.add("Sản phẩm \"" + item.getProduct().getName() + "\" không tồn tại.");
            }
            // Kiểm tra xem số lượng trong kho có ĐỦ không
            else if (productInDb.getQuantity() < item.getQuantity()) {
                errors.add("Không đủ hàng cho \"" + item.getProduct().getName() + "\". " +
                        "Bạn muốn mua " + item.getQuantity() +
                        " nhưng chỉ còn " + productInDb.getQuantity() + " sản phẩm.");
            }
        }
        return errors;
    }

    /**
     * Phương thức duy nhất thực hiện việc tạo và lưu đơn hàng.
     * Nó nhận ID và giỏ hàng, sau đó thực hiện toàn bộ quy trình trong 1 transaction.
     */
//    @Transactional // (Rất quan trọng)
//    public Orders finalizeOrder(Integer customerId, CartBean cart) {
//        // Lấy thông tin khách hàng
//        Customer customer = customerService.getById(customerId);
//        if (customer == null) {
//            throw new RuntimeException("Không tìm thấy khách hàng.");
//        }
//
//        // Tạo và LƯU đối tượng Orders TRƯỚC TIÊN để có ID
//        Orders newOrder = new Orders();
//        newOrder.setCustomer(customer);
//        newOrder.setStatus(OrderStatus.SHIPPING);
//        newOrder.setDate(LocalDateTime.now());
//
//        // Mặc định lấy địa chỉ từ Customer
//        String shippingAddress = customer.getFullAddress();
//
//        // Kiểm tra nếu địa chỉ trống (trường hợp khách chưa cập nhật profile)
//        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
//            throw new RuntimeException("Vui lòng cập nhật địa chỉ giao hàng trong Hồ sơ trước khi đặt hàng.");
//        }
//
//        // Lưu cứng địa chỉ vào đơn hàng (Snaphot)
//        newOrder.setShippingAddress(shippingAddress);
//
//        Orders savedOrder = orderService.createOrder(newOrder);
//
//        //tạo và lưu các đối tượng OrderLine
//        for (CartItemBean item : cart.getItems()) {
//            // Lấy sản phẩm và KHÓA nó lại cho transaction
//            Product product = productService.getById(item.getProduct().getId());
//
//            //Kiểm tra số lượng tồn lần cuối
//            if (product == null || product.getQuantity() < item.getQuantity()) {
//                // Nếu lỗi, @Transactional sẽ tự động rollback (hủy) đơn hàng
//                throw new RuntimeException("Rất tiếc, sản phẩm \"" + item.getProduct().getName() +
//                        "\" không đủ hàng. Chỉ còn " + (product != null ? product.getQuantity() : 0));
//            }
//
//            // Tạo OrderLine (Đã đúng)
//            OrderLineId orderLineId = new OrderLineId(savedOrder.getId(), product.getId());
//            OrderLine newOrderLine = new OrderLine();
//            newOrderLine.setId(orderLineId);
//            newOrderLine.setOrder(savedOrder);
//            newOrderLine.setProduct(product);
//            newOrderLine.setAmount(item.getQuantity());
//            newOrderLine.setPurchasePrice(BigDecimal.valueOf(product.getPrice()));
//
//            orderLineRepository.save(newOrderLine);
//
//
//            // Cập nhật (giảm) số lượng tồn kho
//            productService.reduceStock(product.getId(), item.getQuantity());
//        }
//
//        return savedOrder;
//    }


    @Transactional
    public Orders finalizeOrderCOD(Integer customerId,
                                   CartBean cart,
                                   PaymentMethod paymentMethod,
                                   ShippingMethod shippingMethod,
                                   String addressFromForm,
                                   String noteFromForm,
                                   String couponCode) {

        // 1. Lấy customer
        Customer customer = customerService.getById(customerId);
        if (customer == null) {
            throw new RuntimeException("Không tìm thấy khách hàng.");
        }

        // 2. Xác định địa chỉ cuối
        String finalAddress = (addressFromForm != null && !addressFromForm.trim().isEmpty())
                ? addressFromForm.trim()
                : customer.getFullAddress();

        if (finalAddress == null || finalAddress.isEmpty()) {
            throw new RuntimeException("Vui lòng nhập địa chỉ giao hàng!");
        }

        // 3. Tính phí ship
        BigDecimal shippingFee = calculateBaseShippingFee(finalAddress, shippingMethod);

        BigDecimal discountAmount = BigDecimal.ZERO;

        if (couponCode != null && !couponCode.trim().isEmpty()) {
            // Tìm coupon trong DB
            Coupon coupon = couponService.findByCode(couponCode); // Cần có hàm này trong Repo

            if (coupon != null) {
                // Tính toán tiền giảm (Hàm calculateDiscount đã có logic check hạn, số lượng, điều kiện)
                discountAmount = calculateDiscount(cart, coupon, customerId);

                // Nếu áp dụng thành công (tiền giảm > 0), trừ số lượng coupon
                if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                    coupon.setQuantity(coupon.getQuantity() - 1);
                    couponService.update(coupon.getId(),coupon);
                }
            }
        }

        // 4. Tạo order
        Orders newOrder = new Orders();
        newOrder.setCustomer(customer);
        newOrder.setDate(LocalDateTime.now());
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setPhoneNumber(customer.getPhoneNumber());
        newOrder.setShippingAddress(finalAddress);
        newOrder.setPaymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.COD);
        newOrder.setShippingMethod(shippingMethod != null ? shippingMethod : ShippingMethod.STANDARD);

        // Set các giá trị tiền
        newOrder.setShippingFee(shippingFee);
        newOrder.setDiscountAmount(discountAmount); // <--- [2] LƯU TIỀN GIẢM GIÁ

        newOrder.setDeliveryDate(newOrder.getEstimatedDeliveryDate());
        newOrder.setNote(noteFromForm != null ? noteFromForm.trim() : "");

        newOrder.setOrderLines(new HashSet<>());

        // 5. Lưu order
        Orders savedOrder = ordersRepository.save(newOrder);

        // 6. Tạo OrderLine và add vào Set
        for (CartItemBean item : cart.getItems()) {
            Product product = productService.getById(item.getProduct().getId());

            if (product == null || product.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("Không đủ hàng: " + (product != null ? product.getQuantity() : 0));
            }

            OrderLineId orderLineId = new OrderLineId(savedOrder.getId(), product.getId());
            OrderLine newLine = new OrderLine();
            newLine.setId(orderLineId);
            newLine.setOrder(savedOrder);
            newLine.setProduct(product);
            newLine.setAmount(item.getQuantity());

            // Lưu giá tại thời điểm mua
            newLine.setPurchasePrice(BigDecimal.valueOf(product.getPrice()));

            OrderLine savedLine = orderLineRepository.save(newLine);

            // Add vào quan hệ orderLines
            savedOrder.getOrderLines().add(savedLine);

            // 7. Giảm stock
            productService.reduceStock(product.getId(), item.getQuantity());
        }

        // 8. Update status -> SHIPPING (Tự động duyệt)
        savedOrder.setStatus(OrderStatus.SHIPPING);
        ordersRepository.save(savedOrder);

        // 9. Fetch lại order đầy đủ
        Orders fullOrder = ordersRepository.findFullOrderWithLines(savedOrder.getId());
        if (fullOrder.getOrderLines() == null) {
            fullOrder.setOrderLines(new HashSet<>());
        }

        return fullOrder;
    }


    private BigDecimal calculateBaseShippingFee(String address, ShippingMethod method) {
        boolean isExpress = (method == ShippingMethod.EXPRESS);

        if (address.toLowerCase().contains("quận") || address.toLowerCase().contains("tp")) {
            // Nội thành
            return isExpress ? BigDecimal.valueOf(30000) : BigDecimal.valueOf(15000);
        } else {
            // Ngoại thành
            return isExpress ? BigDecimal.valueOf(50000) : BigDecimal.valueOf(25000);
        }
    }

    // ============================================================
    // LOGIC TÍNH TOÁN GIẢM GIÁ (COUPON)
    // ============================================================
    /**
     * API Public để Controller gọi lấy số tiền giảm giá
     */
    public BigDecimal calculateDiscountAmount(CartBean cart, String couponCode, Integer customerId) {
        if (couponCode == null || couponCode.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Tìm coupon
        Coupon coupon = null;
        try {
            // coupon = couponRepository.findByCode(couponCode);
            List<Coupon> all = couponService.getAll();
            for(Coupon c : all) {
                if(c.getCode().equalsIgnoreCase(couponCode)) {
                    coupon = c;
                    break;
                }
            }
        } catch(Exception e) {}

        if (coupon == null) return BigDecimal.ZERO;

        try {
            return calculateDiscount(cart, coupon, customerId);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    /**
     * Hàm tính toán số tiền được giảm giá dựa trên loại Coupon
     */
    private BigDecimal calculateDiscount(CartBean cart, Coupon coupon, Integer customerId) {

        // 1. Validate cơ bản (Ngày, Số lượng, Active)
        if (!coupon.isActive() || coupon.getQuantity() <= 0) return BigDecimal.ZERO;
        if (coupon.getEndDate() != null && LocalDate.now().isAfter(coupon.getEndDate())) return BigDecimal.ZERO;
        if (coupon.getStartDate() != null && LocalDate.now().isBefore(coupon.getStartDate())) return BigDecimal.ZERO;

        BigDecimal cartTotal = BigDecimal.valueOf(cart.getTotal());
        BigDecimal discount = BigDecimal.ZERO;

        // 2. Xử lý theo từng loại (Đa hình)

        // --- TRƯỜNG HỢP A: GIẢM GIÁ HÓA ĐƠN (OrderCoupon) ---
        if (coupon instanceof OrderCoupon) {
            OrderCoupon orderCoupon = (OrderCoupon) coupon;

            // Kiểm tra đơn tối thiểu
            if (orderCoupon.getMinOrderAmount() != null) {
                if (cartTotal.compareTo(orderCoupon.getMinOrderAmount()) < 0) {
                    // Nếu gọi từ finalizeOrder, ném lỗi để chặn.
                    // Nếu gọi từ trang Cart (để hiển thị), có thể return 0 thay vì throw exception (tùy logic bạn chọn)
                    throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu: " +
                            String.format("%,.0f", orderCoupon.getMinOrderAmount()) + " đ");
                }
            }
            // Tính tiền giảm
            discount = calculateBaseDiscount(cartTotal, coupon);
        }

        // --- TRƯỜNG HỢP B: GIẢM THEO THƯƠNG HIỆU (CategoryCoupon) ---
        else if (coupon instanceof CategoryCoupon) {
            CategoryCoupon catCoupon = (CategoryCoupon) coupon;
            BigDecimal eligibleTotal = BigDecimal.ZERO;

            // Chỉ cộng tiền các sp thuộc category đó
            for (CartItemBean item : cart.getItems()) {
                if (item.getProduct().getCategory().getId().equals(catCoupon.getTargetCategory().getId())) {
                    eligibleTotal = eligibleTotal.add(BigDecimal.valueOf(item.getSubTotal()));
                }
            }

            if (eligibleTotal.compareTo(BigDecimal.ZERO) == 0) {
                throw new RuntimeException("Trong giỏ không có sản phẩm nào thuộc thương hiệu áp dụng mã này.");
            }

            discount = calculateBaseDiscount(eligibleTotal, coupon);
        }

        // --- TRƯỜNG HỢP C: KHÁCH HÀNG MỚI (WelcomeCoupon) ---
        else if (coupon instanceof WelcomeCoupon) {
            // Kiểm tra xem khách đã từng mua đơn nào thành công chưa
            // (Cần inject OrdersRepository vào Service này)
            long count = ordersRepository.countByCustomerId(customerId);
            if (count > 0) {
                throw new RuntimeException("Mã này chỉ dành cho khách hàng mới mua lần đầu.");
            }

            discount = calculateBaseDiscount(cartTotal, coupon);
        }

        // Đảm bảo không giảm quá tổng tiền đơn hàng
        return discount.min(cartTotal);
    }


    /**
     * Hàm phụ trợ tính toán (Tiền/Phần trăm)
     */
    private BigDecimal calculateBaseDiscount(BigDecimal baseAmount, Coupon coupon) {
        if (coupon.isPercentage()) {
            // Tính %: base * (value / 100)
            BigDecimal amount = baseAmount.multiply(coupon.getDiscountValue().divide(BigDecimal.valueOf(100)));

            // Check Max Discount (Giảm tối đa)
            if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                return amount.min(coupon.getMaxDiscountAmount());
            }
            return amount;
        } else {
            // Trừ thẳng tiền mặt
            return coupon.getDiscountValue();
        }
    }

    // ============================================================
    // LOGIC LỌC COUPON (Cho trang Cart)
    // ============================================================

    public List<Coupon> getApplicableCoupons(CartBean cart, Integer customerId) {
        List<Coupon> allCoupons = couponService.getAll();
        List<Coupon> validCoupons = new ArrayList<>();

        for (Coupon coupon : allCoupons) {
            try {
                // 1. Check ngày tháng trước cho nhanh
                if (coupon.getEndDate() != null && LocalDate.now().isAfter(coupon.getEndDate())) continue;
                if (coupon.getStartDate() != null && LocalDate.now().isBefore(coupon.getStartDate())) continue;
                if (coupon.getQuantity() <= 0) continue;

                // 2. Check điều kiện logic (isApplicable được viết trong Entity)
                // Hàm này trả về true/false chứ không ném exception
                if (coupon.isApplicable(cart, customerId, orderService)) {
                    validCoupons.add(coupon);
                }
            } catch (Exception e) {
                // Bỏ qua lỗi nếu có
            }
        }
        return validCoupons;
    }
}