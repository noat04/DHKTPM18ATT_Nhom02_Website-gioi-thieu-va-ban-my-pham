package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.repository.CategoryRepository;
import org.fit.shopnuochoa.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
@Service
public class CategoryService {
    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;
    private CloudinaryService cloudinaryService;

    public CategoryService(CategoryRepository categoryRepository,ProductRepository productRepository,CloudinaryService cloudinaryService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.cloudinaryService = cloudinaryService;
    }
    public List<String> findDistinctCountries(){
        return categoryRepository.findDistinctCountries();
    }
    public List<Category> getAll() {return categoryRepository.findAll();}

    // Phân trang - lấy tất cả
    public Page<Category> getAllPaged(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    // Tìm kiếm theo tên với phân trang
    public Page<Category> searchByName(String keyword, Pageable pageable) {
        return categoryRepository.findByNameContainingIgnoreCase(keyword, pageable);
    }

    public Category getById(Integer id) {return categoryRepository.findById(id).orElse(null);}

    public Category createCategory(Category category, MultipartFile imageFile) {
        if (!imageFile.isEmpty()) {
            String url = cloudinaryService.uploadProductImage(imageFile);
            category.setImgURL(url);
        }

        return categoryRepository.save(category);
    }

    // Cập nhật
    public Optional<Category> updateCategory(int id, Category updatedCategory,MultipartFile imageFile) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(updatedCategory.getName());
            category.setCountry(updatedCategory.getCountry());
            if (!imageFile.isEmpty()) {
                // Nếu có ảnh cũ, có thể xóa đi trước khi up mới (dùng hàm updateProductImage)
                String newUrl = cloudinaryService.updateProductImage(imageFile, category.getImgURL());
                category.setImgURL(newUrl);
            }
            return categoryRepository.save(category);
        });
    }

    public Optional<Category> deleteCategory(int id) {
        // Kiểm tra xem category có tồn tại sản phẩm nào không
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalStateException("Không thể xóa loại sản phẩm này vì đang tồn tại " + productCount + " sản phẩm thuộc loại này!");
        }

        Optional<Category> emp = categoryRepository.findById(id);
        emp.ifPresent(categoryRepository::delete);   // Nếu có thì xoá
        return emp;// Trả về Optional vừa xoá (nếu có)
    }

}
