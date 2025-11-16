package org.fit.shopnuochoa.service;

import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.repository.CategoryRepository;
import org.fit.shopnuochoa.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class CategoryService {
    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository,ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }
    public List<String> findDistinctCountries(){
        return categoryRepository.findDistinctCountries();
    }
    public List<Category> getAll() {return categoryRepository.findAll();}

    public Category getById(Integer id) {return categoryRepository.findById(id).orElse(null);}

    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    // Cập nhật
    public Optional<Category> updateCategory(int id, Category updatedCategory) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(updatedCategory.getName());
            category.setCountry(updatedCategory.getCountry());
            category.setImgURL(updatedCategory.getImgURL());
            return categoryRepository.save(category);
        });
    }

    public Optional<Category> deleteCategory(int id) {
        Optional<Category> emp = categoryRepository.findById(id);
        emp.ifPresent(categoryRepository::delete);   // Nếu có thì xoá
        return emp;// Trả về Optional vừa xoá (nếu có)
    }

}
