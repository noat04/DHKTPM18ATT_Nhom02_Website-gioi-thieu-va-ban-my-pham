package org.fit.shopnuochoa.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fit.shopnuochoa.Enum.Gender;
import org.fit.shopnuochoa.Enum.Volume;
import org.fit.shopnuochoa.component.SecurityUtils;
import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.model.Comment;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@Controller
@RequestMapping("/api/products")
public class ProductController {
    
    private SecurityUtils securityUtils;
    private ProductService productService;
    private CategoryService categoryService;
    private CommentService commentService;
    private WishlistService wishlistService;
    private UserService userService;

    public ProductController(ProductService productService,
                             CategoryService categoryService,
                             CommentService commentService,
                             SecurityUtils securityUtils,
                             WishlistService wishlistService,
                             UserService userService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.commentService = commentService;
        this.securityUtils = securityUtils;
        this.wishlistService = wishlistService;
        this.userService=userService;
    }

    @GetMapping("/list")
    public String showProductList(
            // Tất cả các tham số từ Form
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "country", required = false) List<String> countries, // Sửa 'origin' -> 'country'
            @RequestParam(value = "gender", required = false) Gender gender,
            @RequestParam(value = "volume", required = false) Volume volume,
            @RequestParam(value = "rating", required = false) Double rating,
            // Tham số sắp xếp và phân trang
            @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            Model model,
            Authentication authentication) {

        // 1. Lấy danh sách Categories (Giữ nguyên)
        List<Category> categories = categoryService.getAll();
        List<String> countryName = categoryService.findDistinctCountries();
        model.addAttribute("listCountryName", countryName);
        model.addAttribute("categories", categories);

        // 2. Tạo Sort (Giữ nguyên)
        Sort sortOption = switch (sort) {
            case "priceDesc" -> Sort.by("price").descending();
            case "priceAsc" -> Sort.by("price").ascending();
            case "bestseller" -> Sort.by("ratingCount").descending();
            default -> Sort.by("id").descending(); // "newest"
        };
        Pageable pageable = PageRequest.of(page, size, sortOption);

        // 3. GỌI HÀM TÌM KIẾM TỔNG HỢP (ĐÃ CẬP NHẬT)
        Page<Product> productPage = productService.searchProducts(
                keyword, categoryId, price, maxPrice, countries, volume, gender, rating, pageable
        );

        model.addAttribute("productPage", productPage);

        // 4. Đưa tất cả tham số filter trở lại Model (để form "nhớ" lựa chọn)
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("price", price);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("gender", gender);
        model.addAttribute("volume", volume);
        model.addAttribute("rating", rating);
        model.addAttribute("country", countries);
        model.addAttribute("sort", sort); // Giữ lại sort


        // 5. Kiểm tra quyền Admin (Giữ nguyên)
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return "screen/customer/product-list";
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? "screen/admin/admin-product-list" : "screen/customer/product-list";
    }

    /**
     * [CẬP NHẬT]
     * Logic hiển thị fragment AJAX
     * Đã được tinh gọn để đồng bộ 100% với showProductList
     */
    @GetMapping("/list/fragment")
    public String getProductFragment(
            // Tất cả các tham số từ Form (do JavaScript gửi lên)
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "gender", required = false) Gender gender,
            @RequestParam(value = "volume", required = false) Volume volume,
            @RequestParam(value = "rating", required = false) Double rating,
            @RequestParam(value = "country", required = false) List<String> countries,
            // Tham số sắp xếp và phân trang
            @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            Model model) {

        // 1. Tạo Sort (Giữ nguyên)
        Sort sortOption = switch (sort) {
            case "priceDesc" -> Sort.by("price").descending();
            case "priceAsc" -> Sort.by("price").ascending();
            case "bestseller" -> Sort.by("ratingCount").descending();
            default -> Sort.by("id").descending(); // "newest"
        };
        Pageable pageable = PageRequest.of(page, size, sortOption);

        // 2. GỌI HÀM TÌM KIẾM TỔNG HỢP (ĐÃ CẬP NHẬT)
        Page<Product> productPage = productService.searchProducts(
                keyword, categoryId, price, maxPrice, countries, volume, gender, rating, pageable
        );
        model.addAttribute("productPage", productPage);

        // 3. Đưa tham số filter trở lại (cho fragment nếu cần)
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("price", price);
        model.addAttribute("country", countries); // Thêm dòng này
        model.addAttribute("gender", gender); // <-- QUAN TRỌNG
        model.addAttribute("volume", volume); // <-- QUAN TRỌNG
        model.addAttribute("rating", rating); // <-- QUAN TRỌNG
        // 4. Trả về fragment (Giữ nguyên)
        // (Hãy chắc chắn bạn có file /fragment/product-ajax.html
        // và nó chứa một fragment tên là 'ajaxUpdate')
        return "fragment/product-ajax :: ajaxUpdate";
    }

    // ✅ Đúng
    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String showEmployeeForm(@RequestParam(value = "action", required = false, defaultValue = "add") String action,
                                   @RequestParam(value = "id", required = false) Integer id,
                                   Model model) {
        Product product;
        List<Category> categories = categoryService.getAll();
        if ("edit".equals(action) && id != null) {
            product = productService.getById(id); // Lấy thông tin nhân viên theo id
        } else {
            product = new Product(); // Thêm mới thì tạo mới
        }
        model.addAttribute("categories", categories);
        model.addAttribute("product", product);
        model.addAttribute("action", action);
        return "screen/admin/admin-product-form";
    }


    @PostMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String handleEmployeeForm(@RequestParam("action") String action,
                                     @RequestParam(value = "categoryId", required = false) Integer categoryId,
                                     @RequestParam("imageFile") MultipartFile imageFile, // <-- THÊM NHẬN FILE
                                     Product product) {
        if ("add".equals(action)) {
            // Gọi hàm create mới có tham số imageFile
            productService.createProduct(product, categoryId, imageFile);
        } else if ("edit".equals(action)) {
            // Gọi hàm update mới có tham số imageFile
            productService.updateProduct(product.getId(), product, categoryId, imageFile);
        }
        return "redirect:/api/products/list";
    }

    // Trong file: ProductController.java

// (Hãy chắc chắn bạn đã inject các service này trong Constructor)
// private final UserService userService;
// private final WishlistService wishlistService;

    @GetMapping("/detail/{id}")
    public String showDetail(@PathVariable("id") Integer id,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "5") int size,
                             Authentication authentication, // <-- Sửa: Dùng Authentication (linh hoạt hơn)
                             Model model) {

        Product product = productService.getById(id);
        Double rating = productService.findAverageRatingByProductId(id);
        model.addAttribute("product", product);

        // === [THÊM MỚI] Lấy ID của khách hàng đang đăng nhập ===
        Integer loggedInCustomerId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Users loggedInUser = userService.getUserByUsername(username);
            if (loggedInUser != null && loggedInUser.getCustomer() != null) {
                loggedInCustomerId = loggedInUser.getCustomer().getId();
            }
        }
        // Thêm vào Model để HTML có thể sử dụng
        model.addAttribute("loggedInCustomerId", loggedInCustomerId);
        // === [KẾT THÚC THÊM MỚI] ===


        if (product != null) {
            // ✅ Phân trang cho bình luận (Giữ nguyên)
            Pageable commentPageable = PageRequest.of(page, size);
            Page<Comment> commentPage = commentService.getByProductId(id, commentPageable);
            model.addAttribute("commentPage", commentPage);
            model.addAttribute("productId", id);
            model.addAttribute("rating", rating);

            // ✅ [SỬA LỖI] Cập nhật logic Wishlist (dùng customerId)
            if (loggedInCustomerId != null) {
                boolean isFavorite = wishlistService.existsInWishlist(loggedInCustomerId, id);
                product.setFavorite(isFavorite);
            } else {
                product.setFavorite(false); // Đảm bảo là false nếu chưa đăng nhập
            }

            // ✅ Lấy sản phẩm tương tự (Giữ nguyên)
            List<Product> similarProducts = productService.findSimilarProducts(
                    product.getCategory().getId(), id);
            model.addAttribute("similarProducts", similarProducts);
        }

        return "screen/customer/product-detail";
    }
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=products.xlsx";
        response.setHeader(headerKey, headerValue);

        List<Product> productList = productService.getAll();

        // Tạo workbook
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");

        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("Tên sản phẩm");
        headerRow.createCell(2).setCellValue("Giá");
        headerRow.createCell(3).setCellValue("Nhà sản xuất");
        headerRow.createCell(4).setCellValue("Tồn kho");

        // Fill data
        int rowCount = 1;
        for (Product product : productList) {
            Row row = sheet.createRow(rowCount++);
            row.createCell(0).setCellValue(product.getId());
            row.createCell(1).setCellValue(product.getName());
            row.createCell(2).setCellValue(product.getPrice());
            row.createCell(3).setCellValue(product.getCategory().getName());
//            row.createCell(4).setCellValue(Boolean.TRUE.equals(product.getInStock()) ? "Còn hàng" : "Hết hàng");
        }

        // Auto-size columns
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public String importProducts(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file to upload.");
        }
        productService.importFromExcel(file);
        return "redirect:/api/products/list";
    }

    @GetMapping("/template")
    @PreAuthorize("hasRole('ADMIN')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=product_template.xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Name");
        header.createCell(1).setCellValue("Price");
        header.createCell(2).setCellValue("Category");
        header.createCell(3).setCellValue("InStock");

        workbook.write(response.getOutputStream());
        workbook.close();
    }




}
