package org.fit.shopnuochoa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fit.shopnuochoa.model.OrderLine;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.repository.OrderLineRepository;
import org.fit.shopnuochoa.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service để tính toán thống kê sản phẩm
 * - Sản phẩm bán chạy nhất
 * - Sản phẩm mắt/rẻ nhất
 * - Sản phẩm đánh giá cao nhất
 * - Phân tích xu hướng
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStatisticsService {

    private final OrderLineRepository orderLineRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    /**
     * DTO cho thống kê sản phẩm
     */
    public static class ProductStats {
        private final Product product;
        private final Integer totalSold;
        private final Double totalRevenue;

        public ProductStats(Product product, Integer totalSold, Double totalRevenue) {
            this.product = product;
            this.totalSold = totalSold;
            this.totalRevenue = totalRevenue;
        }

        public Product getProduct() { return product; }
        public Integer getTotalSold() { return totalSold; }
        public Double getTotalRevenue() { return totalRevenue; }
    }

    /**
     * Lấy top sản phẩm bán chạy nhất
     */
    public List<ProductStats> getBestSellingProducts(int limit) {
        List<OrderLine> allOrderLines = orderLineRepository.findAll();

        // Nhóm theo product và tính tổng số lượng bán
        Map<Integer, Integer> productSales = new HashMap<>();
        Map<Integer, Double> productRevenue = new HashMap<>();

        for (OrderLine line : allOrderLines) {
            Integer productId = line.getProduct().getId();
            productSales.merge(productId, line.getAmount(), Integer::sum);
            productRevenue.merge(productId,
                line.getPurchasePrice().doubleValue() * line.getAmount(),
                Double::sum);
        }

        // Sắp xếp và lấy top N
        return productSales.entrySet().stream()
            .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                Product product = productService.getById(entry.getKey());
                return new ProductStats(
                    product,
                    entry.getValue(),
                    productRevenue.get(entry.getKey())
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Lấy sản phẩm mắt nhất
     */
    public List<Product> getMostExpensiveProducts(int limit) {
        return productRepository.findAll().stream()
            .filter(Product::isInStock)
            .sorted((a, b) -> Double.compare(b.getPrice(), a.getPrice()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Lấy sản phẩm rẻ nhất
     */
    public List<Product> getCheapestProducts(int limit) {
        return productRepository.findAll().stream()
            .filter(Product::isInStock)
            .sorted(Comparator.comparingDouble(Product::getPrice))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Lấy sản phẩm đánh giá cao nhất
     */
    public List<Product> getTopRatedProducts(int limit) {
        return productRepository.findAll().stream()
            .filter(p -> p.getAverageRating() != null && p.getAverageRating() > 0)
            .sorted((a, b) -> {
                double ratingA = a.getAverageRating() != null ? a.getAverageRating() : 0;
                double ratingB = b.getAverageRating() != null ? b.getAverageRating() : 0;
                // Sắp xếp theo rating, nếu bằng nhau thì theo số lượt đánh giá
                if (ratingB != ratingA) {
                    return Double.compare(ratingB, ratingA);
                }
                int countA = a.getRatingCount() != null ? a.getRatingCount() : 0;
                int countB = b.getRatingCount() != null ? b.getRatingCount() : 0;
                return Integer.compare(countB, countA);
            })
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Lấy sản phẩm mới nhất
     */
    public List<Product> getNewestProducts(int limit) {
        return productRepository.findAll().stream()
            .filter(Product::isInStock)
            .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Lấy sản phẩm hot trend
     */
    public List<Product> getHotTrendProducts(int limit) {
        return productRepository.findAll().stream()
            .filter(p -> p.getHotTrend() != null && p.getHotTrend())
            .filter(Product::isInStock)
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Tạo context thống kê chi tiết cho RAG
     */
    public String generateEnhancedStatistics() {
        StringBuilder stats = new StringBuilder();

        // Top bán chạy
        List<ProductStats> bestSellers = getBestSellingProducts(5);
        if (!bestSellers.isEmpty()) {
            stats.append("🏆 TOP SẢN PHẨM BÁN CHẠY:\n");
            for (int i = 0; i < bestSellers.size(); i++) {
                ProductStats ps = bestSellers.get(i);
                stats.append(String.format("%d. %s - Đã bán: %d sản phẩm (Doanh thu: %,.0f VNĐ)\n",
                    i + 1, ps.getProduct().getName(), ps.getTotalSold(), ps.getTotalRevenue()));
            }
            stats.append("\n");
        }

        // Top đánh giá cao
        List<Product> topRated = getTopRatedProducts(5);
        if (!topRated.isEmpty()) {
            stats.append("⭐ TOP SẢN PHẨM ĐÁNH GIÁ CAO:\n");
            for (int i = 0; i < topRated.size(); i++) {
                Product p = topRated.get(i);
                stats.append(String.format("%d. %s - %.1f/5 ⭐ (%d lượt)\n",
                    i + 1, p.getName(), p.getAverageRating(), p.getRatingCount()));
            }
            stats.append("\n");
        }

        // Hot trend
        List<Product> hotTrends = getHotTrendProducts(3);
        if (!hotTrends.isEmpty()) {
            stats.append("🔥 SẢN PHẨM HOT TREND:\n");
            hotTrends.forEach(p -> stats.append(String.format("- %s (%,.0f VNĐ)\n",
                p.getName(), p.getPrice())));
            stats.append("\n");
        }

        return stats.toString();
    }


    /**
     * Lấy tổng số lượng đã bán của một sản phẩm
     */
    public Integer getTotalSoldByProductId(Integer productId) {
        List<OrderLine> orderLines = orderLineRepository.findAll();
        return orderLines.stream()
            .filter(line -> line.getProduct().getId().equals(productId))
            .mapToInt(OrderLine::getAmount)
            .sum();
    }
}

