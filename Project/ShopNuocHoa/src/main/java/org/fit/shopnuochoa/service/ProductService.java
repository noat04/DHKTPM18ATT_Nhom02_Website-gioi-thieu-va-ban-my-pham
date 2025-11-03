package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.repository.CategoryRepository;
import org.fit.shopnuochoa.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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


}
