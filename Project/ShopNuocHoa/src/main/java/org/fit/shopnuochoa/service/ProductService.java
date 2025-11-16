package org.fit.shopnuochoa.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fit.shopnuochoa.Enum.Gender;
import org.fit.shopnuochoa.Enum.Volume;
import org.fit.shopnuochoa.dto.RatingStats;
import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.repository.CategoryRepository;
import org.fit.shopnuochoa.repository.CommentRepository;
import org.fit.shopnuochoa.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private CommentRepository commentRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,CommentRepository commentRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
    }

    public List<Product> getAll() {return productRepository.findAll();}

    public Page<Product> getAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    //    public List<Product> getByCategory(Integer categoryId) {return productRepository.findByCategoryId(categoryId);}

    /**
     * Tìm sản phẩm (phân trang) dựa theo Quốc gia của Danh mục
     */
    public Page<Product> getByCategoryCountry(String categoryCountry, Pageable pageable) {
        return productRepository.findByCategoryCountry(categoryCountry, pageable);
    }

    /**
     * Đếm số lượng sản phẩm dựa theo Quốc gia của Danh mục
     */
    public long countByCategoryCountry(String categoryCountry) {
        return productRepository.countByCategoryCountry(categoryCountry);
    }

    public Page<Product> getByCategory(Integer categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    public Product getById(Integer id) {return productRepository.findById(id).orElse(null);}

    public Product  createProduct(Product product, Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        product.setCategory(category);
        return productRepository.save(product);
    }

    // Cập nhật
    public Optional<Product> updateProduct(int id, Product updatedProduct, Integer categoryId) {
        return productRepository.findById(id).map(product -> {
            product.setName(updatedProduct.getName());
            product.setPrice(updatedProduct.getPrice());
            product.setQuantity(updatedProduct.getQuantity());
            product.setHotTrend(updatedProduct.getHotTrend()); // 🆕 cập nhật hot trend
            product.setVolume(updatedProduct.getVolume());
            product.setGender(updatedProduct.getGender());

            if (updatedProduct.getImageUrl() != null && !updatedProduct.getImageUrl().isBlank()) {
                product.setImageUrl(updatedProduct.getImageUrl());
            }

            if (categoryId != null) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));
                product.setCategory(category);
            }

            return productRepository.save(product);
        });
    }

    public Optional<Product> deleteProduct(int id) {
        Optional<Product> emp = productRepository.findById(id);
        emp.ifPresent(productRepository::delete);   // Nếu có thì xoá
        return emp;// Trả về Optional vừa xoá (nếu có)
    }

    public Page<Product> getByPrice(Double price, Pageable pageable) {
        return productRepository.findByPriceGreaterThan(price,pageable);
    }

    public Page<Product> getProductsByCategoryWithPriceGreaterThan(Integer categoryId,Double price, Pageable pageable) {
        return productRepository.findProductsByCategoryWithPriceGreaterThan(categoryId,price,pageable);
    }

    public List<Product> findSimilarProducts(Integer categoryId, Integer productId) {
        List<Product> allSimilar = productRepository.findByCategoryIdAndIdNot(categoryId, productId);
        // Trả về tối đa 4 sản phẩm
        return allSimilar.stream().toList();
    }

    /**
     * Tìm kiếm sản phẩm tổng hợp (dùng cho cả tải trang và AJAX)
     * Nhận TẤT CẢ các tham số lọc từ controller.
     */
    public Page<Product> searchProducts(String keyword,
                                        Integer categoryId,
                                        Double price,
                                        Double maxPrice,
                                        List<String> countries, // Sửa ở đây
                                        Volume volume,
                                        Gender gender,
                                        Double rating,
                                        Pageable pageable) {

        return productRepository.searchProducts(
                keyword, categoryId, price, maxPrice, countries, volume, gender, rating, pageable
        );
    }

//    public Page<Product> searchProducts(String keyword, Integer categoryId, Double price, Pageable pageable){
//        return productRepository.searchProducts(keyword, categoryId, price, pageable);
//    }

    public void importFromExcel(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        // Validate header
        Row header = sheet.getRow(0);
        if (header == null
                || !"Name".equals(header.getCell(0).getStringCellValue())
                || !"Price".equals(header.getCell(1).getStringCellValue())
                || !"Category".equals(header.getCell(2).getStringCellValue())
                || !"InStock".equals(header.getCell(3).getStringCellValue())) {
            workbook.close();
            throw new IllegalArgumentException("File không đúng template!");
        }

        List<Product> products = new ArrayList<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String name = row.getCell(0).getStringCellValue();
            if (name == null || name.isBlank()) {
                workbook.close();
                throw new IllegalArgumentException("Tên sản phẩm tại dòng " + (i+1) + " không hợp lệ");
            }

            double price = row.getCell(1).getNumericCellValue();
            if (price < 0) {
                workbook.close();
                throw new IllegalArgumentException("Giá sản phẩm tại dòng " + (i+1) + " không hợp lệ");
            }

            String categoryName = row.getCell(2).getStringCellValue();
            if (categoryName == null || categoryName.isBlank()) {
                workbook.close();
                throw new IllegalArgumentException("Category tại dòng " + (i+1) + " không hợp lệ");
            }

            // Nếu không tìm thấy category thì tạo mới
            Category category = (Category) categoryRepository.findByName(categoryName)
                    .orElseGet(() -> {
                        Category newCategory = new Category();
                        newCategory.setName(categoryName);
                        return categoryRepository.save(newCategory);
                    });

            boolean inStock = row.getCell(3).getBooleanCellValue();

            Product product = new Product();
            product.setName(name);
            product.setPrice(price);
            product.setCategory(category);
//            product.setInStock(inStock);

            products.add(product);
        }


        productRepository.saveAll(products);
        workbook.close();
    }

    public Double findAverageRatingByProductId(Integer productId) {
        Double avg = commentRepository.findAverageRatingByProductId(productId);
        return avg != null ? avg : 0.0;
    }

    @Transactional
    public void updateRatingStats(Integer productId) {
        // 1. Tìm sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

        // 2. Gọi câu truy vấn JPQL hiệu quả (từ ProductRepository)
        RatingStats stats = productRepository.getRatingStatsByProductId(productId);

        // 3. Cập nhật và lưu lại
        if (stats != null) {
            product.setAverageRating(stats.average());
            product.setRatingCount(stats.count().intValue()); // Chuyển từ Long sang Integer
        } else {
            // Xử lý trường hợp không có rating nào (hoặc lỗi)
            product.setAverageRating(0.0);
//            product.setRatingCount(0);
        }

        productRepository.save(product);
    }

    @Transactional // Kiểm tra tồn kho
    public void reduceStock(Integer productId, int quantityToReduce) {
        // 1. Tìm sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // 2. Kiểm tra tồn kho
        int currentStock = product.getQuantity();
        if (currentStock < quantityToReduce) {
            // Nếu không đủ hàng, ném ra lỗi
            throw new RuntimeException("Không đủ hàng tồn kho cho sản phẩm: " + product.getName());
        }

        // 3. Trừ và lưu lại
        product.setQuantity(currentStock - quantityToReduce);
        productRepository.save(product);
    }
}
