package org.fit.shopnuochoa.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fit.shopnuochoa.component.SecurityUtils;
import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.model.Comment;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.service.CategoryService;
import org.fit.shopnuochoa.service.CommentService;
import org.fit.shopnuochoa.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    public ProductController(ProductService productService, CategoryService categoryService,CommentService commentService,SecurityUtils securityUtils) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.commentService = commentService;
        this.securityUtils = securityUtils;
    }
    // ✅ Đúng
    @GetMapping("/list")
    public String showProductList(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "price", required = false) Double salary,
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model,
            Authentication authentication) {

        // ✅ Lấy danh sách loại sản phẩm
        List<Category> categories = categoryService.getAll();

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = null;

        if (action == null) action = "list";

        switch (action) {
            case "add":
                productPage = productService.getAll(pageable);
                break;
            case "list":
                productPage = productService.searchProducts(keyword, categoryId, salary, pageable);
                break;
            case "delete":
                // ✅ Chỉ admin mới được xóa
                if (authentication != null && authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                    productService.deleteProduct(id);
                }
                productPage = productService.getAll(pageable);
                break;
            default:
                System.out.println("Unknown action: " + action);
                break;
        }

        // ✅ Gửi dữ liệu sang view
        model.addAttribute("categories", categories);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("price", salary);
        model.addAttribute("productPage", productPage);
        model.addAttribute("keyword", keyword);
        // ✅ Kiểm tra trạng thái đăng nhập
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            // Người chưa đăng nhập (khách)
            return "screen/customer/product-list";
        }

        // ✅ Nếu đã đăng nhập, kiểm tra role
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return "screen/admin/admin-product-list";
        } else {
            return "screen/customer/product-list";
        }
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
                                     Product product) {
        if ("add".equals(action)) {
            productService.createProduct(product, categoryId);
        } else if ("edit".equals(action)) {
            productService.updateProduct(product.getId(), product, categoryId);
        }
        return "redirect:/api/products/list";
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public String showDetail(@PathVariable("id") Integer id,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "5") int size,
                             Model model) {
        Product product = productService.getById(id);
        model.addAttribute("product", product);

        if (product != null) {
            // Lấy danh sách bình luận (với phân trang riêng)
            Pageable commentPageable = PageRequest.of(page, size);
            Page<Comment> commentPage = commentService.getByProductId(id, commentPageable);
            model.addAttribute("commentPage", commentPage);
            model.addAttribute("productId", id);

            // ✅ Lấy danh sách sản phẩm tương tự (không phân trang)
            List<Product> similarProducts = productService.findSimilarProducts(
                    product.getCategory().getId(),
                    id);
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
            row.createCell(4).setCellValue(Boolean.TRUE.equals(product.getInStock()) ? "Còn hàng" : "Hết hàng");
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
