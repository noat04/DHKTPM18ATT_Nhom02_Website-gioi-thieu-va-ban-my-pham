package org.fit.shopnuochoa.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.repository.CategoryRepository;
import org.fit.shopnuochoa.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Product> getAll() {return productRepository.findAll();}
    public Page<Product> getAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
//    public List<Product> getByCategory(Integer categoryId) {return productRepository.findByCategoryId(categoryId);}
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
            product.setInStock(updatedProduct.getInStock());
            // Cập nhật Category một cách an toàn
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
     * Tìm sản phẩm theo tên bằng keyword
     * @param keyword
     * @return danh sách sản phẩm chứa keyword
     */
    public Page<Product> searchProducts(String keyword, Integer categoryId, Double price, Pageable pageable){
        return productRepository.searchProducts(keyword, categoryId, price, pageable);
    }
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
            product.setInStock(inStock);

            products.add(product);
        }


        productRepository.saveAll(products);
        workbook.close();
    }


}
