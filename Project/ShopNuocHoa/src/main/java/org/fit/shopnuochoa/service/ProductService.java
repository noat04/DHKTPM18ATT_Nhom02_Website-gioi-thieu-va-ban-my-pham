package org.fit.shopnuochoa.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fit.shopnuochoa.Enum.Gender;
import org.fit.shopnuochoa.Enum.Volume;
import org.fit.shopnuochoa.dto.ProductImportDTO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private CommentRepository commentRepository;
    private CloudinaryService cloudinaryService;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,CommentRepository commentRepository, CloudinaryService cloudinaryService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.cloudinaryService = cloudinaryService;
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

    public Product  createProduct(Product product, Integer categoryId, MultipartFile imageFile) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        product.setCategory(category);

        // 2. Xử lý Upload ảnh (MỚI)
        if (!imageFile.isEmpty()) {
            String url = cloudinaryService.uploadProductImage(imageFile);
            product.setImageUrl(url);
        }

        return productRepository.save(product);
    }

    // Cập nhật
    public Optional<Product> updateProduct(int id, Product updatedProduct, Integer categoryId,MultipartFile imageFile) {
        return productRepository.findById(id).map(product -> {
            product.setName(updatedProduct.getName());
            product.setPrice(updatedProduct.getPrice());
            product.setQuantity(updatedProduct.getQuantity());
            product.setHotTrend(updatedProduct.getHotTrend()); // 🆕 cập nhật hot trend
            product.setVolume(updatedProduct.getVolume());
            product.setGender(updatedProduct.getGender());
            product.setDescription(updatedProduct.getDescription());
            if (!imageFile.isEmpty()) {
                // Nếu có ảnh cũ, có thể xóa đi trước khi up mới (dùng hàm updateProductImage)
                String newUrl = cloudinaryService.updateProductImage(imageFile, product.getImageUrl());
                product.setImageUrl(newUrl);
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
        emp.ifPresent(product -> {
            product.setDeleted(true);
            productRepository.save(product);
        });
        return emp;
    }

    public Optional<Product> restoreProduct(int id) {
        Optional<Product> product = productRepository.findById(id);
        product.ifPresent(p -> {
            p.setDeleted(false);
            productRepository.save(p);
        });
        return product;
    }

    public Page<Product> getAllDeleted(Pageable pageable) {
        return productRepository.findAllDeleted(pageable);
    }

    public long countDeleted() {
        return productRepository.countDeleted();
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

    // ============= NEW IMPORT FLOW METHODS =============

    /**
     * Preview Excel data without importing
     */
    public List<ProductImportDTO> previewExcelData(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        // Validate header
        Row header = sheet.getRow(0);
        if (header == null || !"Name".equals(getCellValueAsString(header.getCell(0)))) {
            workbook.close();
            throw new IllegalArgumentException("File không đúng template!");
        }

        List<ProductImportDTO> products = new ArrayList<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String name = getCellValueAsString(row.getCell(0));
            if (name == null || name.isBlank()) continue;

            ProductImportDTO dto = new ProductImportDTO();
            dto.setName(name);
            dto.setPrice(getCellValueAsDouble(row.getCell(1)));

            // Category: Lưu cả name và resolve id
            String categoryName = getCellValueAsString(row.getCell(2));
            dto.setCategory(categoryName);
            if (categoryName != null && !categoryName.isBlank()) {
                // Resolve categoryId ngay
                categoryRepository.findByName(categoryName.trim()).ifPresent(cat -> {
                    dto.setCategoryId(cat.getId());
                });
            }

            // Parse Volume - Hỗ trợ cả 2 format: "10ml" và "ML_10"
            String volumeStr = getCellValueAsString(row.getCell(3));
            if (volumeStr != null && !volumeStr.isBlank()) {
                try {
                    // Nếu user nhập "10ml", "30ml"... thì convert sang "ML_10", "ML_30"...
                    String normalizedVolume = volumeStr.trim().toUpperCase();
                    if (normalizedVolume.matches("\\d+ML")) {
                        // Format: "10ML" -> "ML_10"
                        normalizedVolume = "ML_" + normalizedVolume.replace("ML", "");
                    }
                    dto.setVolume(Volume.valueOf(normalizedVolume));
                } catch (Exception e) {
                    // Ignore invalid volume
                }
            }

            // Parse Gender
            String genderStr = getCellValueAsString(row.getCell(4));
            if (genderStr != null && !genderStr.isBlank()) {
                try {
                    dto.setGender(Gender.valueOf(genderStr));
                } catch (Exception e) {
                    // Ignore invalid gender
                }
            }

            dto.setQuantity(getCellValueAsInteger(row.getCell(5)));
            dto.setHotTrend(getCellValueAsBoolean(row.getCell(6)));

            products.add(dto);
        }

        workbook.close();
        return products;
    }

    /**
     * Upload images to Cloudinary and return map of filename -> URL
     */
    public Map<String, String> uploadImagesToCloudinary(List<MultipartFile> images) throws IOException {
        Map<String, String> imageMap = new HashMap<>();

        for (MultipartFile image : images) {
            if (!image.isEmpty()) {
                String originalFilename = image.getOriginalFilename();
                String cloudinaryUrl = cloudinaryService.uploadProductImage(image);

                // Lưu mapping: tên file gốc (không extension) -> Cloudinary URL
                String nameWithoutExt = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
                imageMap.put(nameWithoutExt, cloudinaryUrl);
            }
        }

        return imageMap;
    }

    /**
     * Import products from DTO list (after preview and image matching)
     */
    @Transactional
    public void importProductsFromDTO(List<ProductImportDTO> dtos) {
        List<Product> products = new ArrayList<>();

        for (ProductImportDTO dto : dtos) {
            // Find or create category
            Category category = (Category) categoryRepository.findByName(dto.getCategory())
                    .orElseGet(() -> {
                        Category newCategory = new Category();
                        newCategory.setName(dto.getCategory());
                        return categoryRepository.save(newCategory);
                    });

            Product product = new Product();
            product.setName(dto.getName());
            product.setPrice(dto.getPrice());
            product.setCategory(category);
            product.setVolume(dto.getVolume());
            product.setGender(dto.getGender());
            product.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 0);
            product.setHotTrend(dto.getHotTrend() != null && dto.getHotTrend());
            product.setImageUrl(dto.getImageUrl()); // Set Cloudinary URL

            products.add(product);
        }

        productRepository.saveAll(products);
    }

    // Helper methods for Excel cell reading
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue());
                } catch (Exception e) {
                    yield 0.0;
                }
            }
            default -> 0.0;
        };
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return 0;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue());
                } catch (Exception e) {
                    yield 0;
                }
            }
            default -> 0;
        };
    }

    private Boolean getCellValueAsBoolean(Cell cell) {
        if (cell == null) return false;
        return switch (cell.getCellType()) {
            case BOOLEAN -> cell.getBooleanCellValue();
            case STRING -> "true".equalsIgnoreCase(cell.getStringCellValue());
            default -> false;
        };
    }

    // ============= OLD IMPORT METHOD (Deprecated) =============

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

        // 2. Gọi câu truy vấn JPQL
        RatingStats stats = productRepository.getRatingStatsByProductId(productId);

        // 3. [SỬA LỖI] Kiểm tra null an toàn
        if (stats != null) {
            // Nếu average là null (do không có comment), gán bằng 0.0
            double avg = stats.average() != null ? stats.average() : 0.0;

            // Nếu count là null, gán bằng 0
            int count = stats.count() != null ? stats.count().intValue() : 0;

            product.setAverageRating(avg);
            product.setRatingCount(count);
        } else {
            // Trường hợp stats hoàn toàn null (hiếm gặp nhưng an toàn)
            product.setAverageRating(0.0);
            product.setRatingCount(0);
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

    public long count() {
        return productRepository.count();
    }

    public long countInStock() {
        return productRepository.countInStock();
    }

    public long countOutOfStock() {
        return productRepository.countOutOfStock();
    }
}
