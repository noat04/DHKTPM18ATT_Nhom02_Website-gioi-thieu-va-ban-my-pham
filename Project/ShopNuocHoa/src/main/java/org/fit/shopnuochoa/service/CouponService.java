package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.*;
import org.fit.shopnuochoa.repository.CouponRepository;
import org.fit.shopnuochoa.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    /** Lấy toàn bộ coupon */
    public List<Coupon> getAll() {
        return couponRepo.findAll();
    }

    public Coupon findByCode(String code) {
        return couponRepo.findByCode(code);
    }
    /** Thêm coupon mới (đã phân loại đúng type trong entity) */
    public Coupon create(Coupon coupon) {
        return couponRepo.save(coupon);
    }

    /** Cập nhật coupon theo ID */
    public Coupon update(Integer id, Coupon updated) {
        Coupon coupon = couponRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon không tồn tại!"));

        // cập nhật field chung
        coupon.setCode(updated.getCode());
        coupon.setDescription(updated.getDescription());
        coupon.setImageUrl(updated.getImageUrl());
        coupon.setPercentage(updated.isPercentage());
        coupon.setDiscountValue(updated.getDiscountValue());
        coupon.setMaxDiscountAmount(updated.getMaxDiscountAmount());
        coupon.setStartDate(updated.getStartDate());
        coupon.setEndDate(updated.getEndDate());
        coupon.setQuantity(updated.getQuantity());
        coupon.setActive(updated.isActive());

        // cập nhật field riêng của từng loại
        if (coupon instanceof OrderCoupon o && updated instanceof OrderCoupon u) {
            o.setMinOrderAmount(u.getMinOrderAmount());
        }

        if (coupon instanceof CategoryCoupon c && updated instanceof CategoryCoupon u) {
            if (u.getTargetCategory() != null) {
                c.setTargetCategory(
                        categoryRepo.findById(u.getTargetCategory().getId())
                                .orElseThrow(() -> new RuntimeException("Category không tồn tại!"))
                );
            }
        }

        return couponRepo.save(coupon);
    }

    /** Xóa coupon */
    public void delete(Integer id) {
        if (!couponRepo.existsById(id)) {
            throw new RuntimeException("Coupon không tồn tại!");
        }
        couponRepo.deleteById(id);
    }

    /** Lấy 1 coupon theo ID */
    public Coupon getById(Integer id) {
        return couponRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon không tồn tại!"));
    }

    public List<Coupon> findApplicableCoupons(CartBean cart, Integer customerId) {
        return couponRepo.findAll().stream()
                .filter(cp -> cp.isApplicable(cart, customerId))
                .filter(Coupon::isActive)
                .filter(cp -> cp.getQuantity() > 0)
                .collect(Collectors.toList());
    }
}
