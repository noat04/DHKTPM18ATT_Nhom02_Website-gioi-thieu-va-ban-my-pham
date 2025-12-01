package org.fit.shopnuochoa.model;

import java.util.ArrayList;
import java.util.List;

public class CartBean {
    private List<CartItemBean> items;

    public CartBean() {
        items = new ArrayList<>();
    }

    public List<CartItemBean> getItems() {
        return items;
    }

    // (Hàm này đã đúng, giữ nguyên - đếm số 'loại' sản phẩm)
    public int getTotalItems() {
        int total = 0;
        for (CartItemBean item : items) {
            total += 1;
        }
        return total;
    }

    /**
     * NÂNG CẤP: Thêm 1 sản phẩm và KIỂM TRA TỒN KHO.
     * @param p Product (phải chứa thông tin 'quantity' tồn kho)
     */
    public void addProduct(Product p) {
        // 1. Kiểm tra xem sản phẩm có thực sự còn hàng không
        if (p.getQuantity() <= 0) {
            throw new RuntimeException("Sản phẩm \"" + p.getName() + "\" đã hết hàng.");
        }

        // 2. Tìm sản phẩm trong giỏ
        for (CartItemBean item : items) {
            if (item.getProduct().getId().equals(p.getId())) {

                // 3. KIỂM TRA TỒN KHO: Kiểm tra xem có thể thêm 1 nữa không
                int soLuongMuonThem = item.getQuantity() + 1;
                if (p.getQuantity() < soLuongMuonThem) {
                    throw new RuntimeException("Không đủ hàng! Bạn đã có " + item.getQuantity() +
                            " trong giỏ, không thể thêm. Chỉ còn " + p.getQuantity() + " sản phẩm.");
                }

                // Tồn kho OK -> Tăng số lượng
                item.setQuantity(soLuongMuonThem);
                return;
            }
        }

        // 4. Nếu không tìm thấy (sản phẩm mới), thêm vào giỏ với số lượng 1
        // (Chúng ta đã kiểm tra p.getQuantity() > 0 ở bước 1)
        items.add(new CartItemBean(p, 1));
    }

    // xóa sản phẩm (Hàm này đã đúng, giữ nguyên)
    public void removeProduct(int productId) {
        items.removeIf(item -> item.getProduct().getId() == productId);
    }

    /**
     * NÂNG CẤP: Cập nhật số lượng và KIỂM TRA TỒN KHO.
     */
    public void updateQuantity(int productId, int quantity) {
        if (quantity <= 0) {
            // nếu nhập <= 0 thì xóa luôn sản phẩm
            removeProduct(productId);
            return;
        }

        for (CartItemBean item : items) {
            if (item.getProduct().getId() == productId) {
                // Đã tìm thấy, lấy thông tin Product để kiểm tra tồn kho
                Product p = item.getProduct();

                // KIỂM TRA TỒN KHO
                if (p.getQuantity() < quantity) {
                    // Nếu không đủ hàng, ném lỗi
                    throw new RuntimeException("Không đủ hàng! Chỉ còn " + p.getQuantity() +
                            " sản phẩm. Không thể cập nhật lên " + quantity + ".");
                }

                // Tồn kho OK -> Cập nhật
                item.setQuantity(quantity);
                return;
            }
        }
    }

    // tính tổng tiền (Hàm này đã đúng, giữ nguyên)
    public double getTotal() {
        double total = 0;
        for (CartItemBean item : items) {
            total += item.getSubTotal();
        }
        return total;
    }

    // xóa hết giỏ hàng (Hàm này đã đúng, giữ nguyên)
    public void clear() {
        items.clear();
    }
}

