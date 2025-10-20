package org.fit.shopnuochoa.model;

import java.util.ArrayList;
import java.util.List;

public class CartBean {
    private List<CartItemBean> items;

    public CartBean() {
        items= new ArrayList<>();
    }
    public List<CartItemBean> getItems() {
        return items;
    }
    public int getTotalItems(){
        int total = 0;
        for (CartItemBean item : items){
            total += 1;
        }
        return total;
    }
    public void addProduct(Product p) {
        for (CartItemBean item : items) {
            if (item.getProduct().getId().equals(p.getId())) {
                 item.setQuantity(item.getQuantity() + 1);
                 return;
                 }
        }
        items.add(new CartItemBean(p, 1));
    }
     // xóa sản phẩm
    public void removeProduct(int productId) {
         items.removeIf(item -> item.getProduct().getId() == productId);
    }
         // cập nhật số lượng
         public void updateQuantity(int productId, int quantity) {
         for (CartItemBean item : items) {
            if (item.getProduct().getId() == productId) {
                 if (quantity > 0) {
                     item.setQuantity(quantity);
                    } else {
                     // nếu nhập <= 0 thì xóa luôn sản phẩm
                     removeProduct(productId);
                    }
                 return;
            }
            }
        }

        // tính tổng tiền
         public double getTotal() {
             double total = 0;
             for (CartItemBean item : items) {
                 total += item.getSubTotal();
                 }
             return total;
         }

        // xóa hết giỏ hàng
        public void clear() {
            items.clear();
        }
}

