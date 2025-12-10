package org.fit.shopnuochoa.controller;

import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.service.CategoryService;
import org.fit.shopnuochoa.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/api")
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public HomeController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }


    @GetMapping("/home")
    public String HomePage(Model model) {

        // 1. Lấy Thương hiệu (Categories)
        List<Category> categories = categoryService.getAll();
        model.addAttribute("featuredCategories", categories);

        // 2. Lấy Sản phẩm MỚI NHẤT (Sắp xếp theo ID giảm dần, lấy 8 sản phẩm)
        Pageable pageableNewest = PageRequest.of(0, 8, Sort.by("id").descending());
        List<Product> newProducts = productService.getAll(pageableNewest).getContent();
        model.addAttribute("newProducts", newProducts);

        // 3. Lấy Sản phẩm BÁN CHẠY (Sắp xếp theo ratingCount giảm dần)
        // Dùng 'ratingCount' để đồng bộ với logic trong ProductController
        Pageable pageableBestSelling = PageRequest.of(0, 8, Sort.by("ratingCount").descending());
        List<Product> bestSellingProducts = productService.getAll(pageableBestSelling).getContent();
        model.addAttribute("bestSellingProducts", bestSellingProducts);

        return "screen/customer/home";
    }
}