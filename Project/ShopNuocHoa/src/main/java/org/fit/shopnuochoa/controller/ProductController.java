package org.fit.shopnuochoa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fit.shopnuochoa.Enum.Gender;
import org.fit.shopnuochoa.Enum.Volume;
import org.fit.shopnuochoa.component.SecurityUtils;
import org.fit.shopnuochoa.dto.ProductImportDTO;
import org.fit.shopnuochoa.model.Category;
import org.fit.shopnuochoa.model.Comment;
import org.fit.shopnuochoa.model.Product;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    public String handleEmployeeForm(
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @Valid @ModelAttribute("product") Product product,
            BindingResult result,
            RedirectAttributes ra,
            Model model) {

        // 1. Kiểm tra lỗi Validation (Dữ liệu nhập vào form)
        if (result.hasErrors()) {
            // Load lại danh sách Category để dropdown không bị lỗi
            model.addAttribute("categories", categoryService.getAll());
            return "screen/admin/admin-product-form"; // Trả về form để hiện lỗi
        }

        try {
            // Lấy categoryId từ object product (do form đã binding vào category.id)
            // Nếu product.getCategory() null (do lỗi binding), nó sẽ bị bắt ở catch
            Integer categoryId = (product.getCategory() != null) ? product.getCategory().getId() : null;

            if ("add".equals(action)) {
                // Thêm mới
                productService.createProduct(product, categoryId, imageFile);
                ra.addFlashAttribute("successMessage", "Thêm sản phẩm thành công!");
            } else {
                // Cập nhật
                productService.updateProduct(product.getId(), product, categoryId, imageFile);
                ra.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            // 2. Bắt lỗi Logic/Hệ thống (Lỗi upload ảnh, lỗi DB...)
            model.addAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            model.addAttribute("categories", categoryService.getAll()); // Đừng quên load lại category
            return "screen/admin/admin-product-form";
        }

        return "redirect:/api/products/list";
    }

    /**
     * Delete Product
     */
    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteProduct(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            productService.deleteProduct(id);
            ra.addFlashAttribute("successMessage", "Xóa sản phẩm thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không thể xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/api/products/list";
    }


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
        response.setHeader("Content-Disposition", "attachment; filename=products.xlsx");

        List<Product> productList = productService.getAll();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");

        // Hyperlink style
        CellStyle hyperlinkStyle = workbook.createCellStyle();
        Font hyperlinkFont = workbook.createFont();
        hyperlinkFont.setUnderline(Font.U_SINGLE);
        hyperlinkFont.setColor(IndexedColors.BLUE.getIndex());
        hyperlinkStyle.setFont(hyperlinkFont);

        // Header
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("Name");
        headerRow.createCell(2).setCellValue("ImageUrl");
        headerRow.createCell(3).setCellValue("Price");
        headerRow.createCell(4).setCellValue("Category");
        headerRow.createCell(5).setCellValue("Volume");
        headerRow.createCell(6).setCellValue("Gender");
        headerRow.createCell(7).setCellValue("Quantity");
        headerRow.createCell(8).setCellValue("HotTrend");

        CreationHelper creationHelper = workbook.getCreationHelper();
        int rowCount = 1;

        for (Product product : productList) {
            Row row = sheet.createRow(rowCount++);

            row.createCell(0).setCellValue(product.getId());
            row.createCell(1).setCellValue(product.getName());

            // --- IMAGE URL as HYPERLINK ---
            Cell imgCell = row.createCell(2);
            String imageUrl = product.getImagePath(); // Cloudinary URL
            imgCell.setCellValue(imageUrl);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
                hyperlink.setAddress(imageUrl);
                imgCell.setHyperlink(hyperlink);
                imgCell.setCellStyle(hyperlinkStyle);
            }

            row.createCell(3).setCellValue(product.getPrice());
            row.createCell(4).setCellValue(product.getCategory().getName());
            row.createCell(5).setCellValue(product.getVolume() != null ? product.getVolume().name() : "");
            row.createCell(6).setCellValue(product.getGender() != null ? product.getGender().name() : "");
            row.createCell(7).setCellValue(product.getQuantity() != null ? product.getQuantity() : 0);
            row.createCell(8).setCellValue(Boolean.TRUE.equals(product.getHotTrend()) ? "true" : "false");
        }

        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // ============= NEW IMPORT FLOW =============

    /**
     * Step 1: Show import preview page
     */
    @GetMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public String showImportPage(Model model) {
        model.addAttribute("categories", categoryService.getAll());
        return "screen/admin/admin-product-import";
    }

    /**
     * Step 2: Preview Excel data
     */
    @PostMapping("/import/preview")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public List<ProductImportDTO> previewExcel(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file to upload.");
        }
        return productService.previewExcelData(file);
    }

    /**
     * Step 3: Upload images and return URLs
     */
    @PostMapping("/import/upload-images")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Map<String, String> uploadImages(@RequestParam("images") List<MultipartFile> images) throws IOException {
        return productService.uploadImagesToCloudinary(images);
    }

    /**
     * Get modal fragment for editing product
     */
    @GetMapping("/import/edit-modal")
    @PreAuthorize("hasRole('ADMIN')")
    public String getEditModal(@RequestParam("index") int index,
                                @RequestParam("productJson") String productJson,
                                Model model) {
        try {
            // Parse product từ JSON với ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            // Cấu hình để parse enum đúng cách
            mapper.findAndRegisterModules();

            ProductImportDTO product = mapper.readValue(productJson, ProductImportDTO.class);

            model.addAttribute("product", product);
            model.addAttribute("index", index);
            model.addAttribute("categories", categoryService.getAll());

            return "fragment/product-import-edit-modal :: edit-modal";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Không thể load modal: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Step 4: Final import with matched data
     */
    @PostMapping("/import/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmImport(@RequestBody List<ProductImportDTO> products) {
        try {
            productService.importProductsFromDTO(products);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Import thành công " + products.size() + " sản phẩm!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============= OLD IMPORT (Deprecated) =============

    @PostMapping("/import-old")
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

        // Create CellStyle for header
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Header row
        Row header = sheet.createRow(0);
        String[] headers = {"Name", "Price", "Category", "Volume", "Gender", "Quantity", "HotTrend"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Example rows
        Row example1 = sheet.createRow(1);
        example1.createCell(0).setCellValue("Dior Sauvage");
        example1.createCell(1).setCellValue(2500000);
        example1.createCell(2).setCellValue("Dior");
        example1.createCell(3).setCellValue("ML_100");
        example1.createCell(4).setCellValue("NAM");
        example1.createCell(5).setCellValue(50);
        example1.createCell(6).setCellValue("true");

        Row example2 = sheet.createRow(2);
        example2.createCell(0).setCellValue("Chanel No 5");
        example2.createCell(1).setCellValue(3200000);
        example2.createCell(2).setCellValue("Chanel");
        example2.createCell(3).setCellValue("ML_50");
        example2.createCell(4).setCellValue("NU");
        example2.createCell(5).setCellValue(30);
        example2.createCell(6).setCellValue("false");

        // ========== DATA VALIDATION (DROPDOWN LISTS) ==========
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();

        // 1. Category Dropdown - Lấy từ database
        List<Category> categories = categoryService.getAll();
        String[] categoryNames = categories.stream()
                .map(Category::getName)
                .toArray(String[]::new);

        if (categoryNames.length > 0) {
            CellRangeAddressList categoryRange = new CellRangeAddressList(1, 1000, 2, 2); // Column C (Category)
            DataValidationConstraint categoryConstraint = validationHelper.createExplicitListConstraint(categoryNames);
            DataValidation categoryValidation = validationHelper.createValidation(categoryConstraint, categoryRange);
            categoryValidation.setShowErrorBox(true);
            categoryValidation.createErrorBox("Lỗi", "Vui lòng chọn Category từ danh sách!");
            categoryValidation.setEmptyCellAllowed(false);
            sheet.addValidationData(categoryValidation);
        }

        // 2. Volume Dropdown
        String[] volumes = {"ML_10", "ML_30", "ML_50", "ML_75", "ML_100", "ML_150", "ML_200"};
        CellRangeAddressList volumeRange = new CellRangeAddressList(1, 1000, 3, 3); // Column D (Volume)
        DataValidationConstraint volumeConstraint = validationHelper.createExplicitListConstraint(volumes);
        DataValidation volumeValidation = validationHelper.createValidation(volumeConstraint, volumeRange);
        volumeValidation.setShowErrorBox(true);
        volumeValidation.createErrorBox("Lỗi", "Vui lòng chọn Volume từ danh sách!");
        volumeValidation.setEmptyCellAllowed(false);
        sheet.addValidationData(volumeValidation);

        // 3. Gender Dropdown
        String[] genders = {"NAM", "NU", "UNISEX"};
        CellRangeAddressList genderRange = new CellRangeAddressList(1, 1000, 4, 4); // Column E (Gender)
        DataValidationConstraint genderConstraint = validationHelper.createExplicitListConstraint(genders);
        DataValidation genderValidation = validationHelper.createValidation(genderConstraint, genderRange);
        genderValidation.setShowErrorBox(true);
        genderValidation.createErrorBox("Lỗi", "Vui lòng chọn Gender từ danh sách!");
        genderValidation.setEmptyCellAllowed(false);
        sheet.addValidationData(genderValidation);

        // 4. HotTrend Dropdown
        String[] hotTrends = {"true", "false"};
        CellRangeAddressList hotTrendRange = new CellRangeAddressList(1, 1000, 6, 6); // Column G (HotTrend)
        DataValidationConstraint hotTrendConstraint = validationHelper.createExplicitListConstraint(hotTrends);
        DataValidation hotTrendValidation = validationHelper.createValidation(hotTrendConstraint, hotTrendRange);
        hotTrendValidation.setShowErrorBox(true);
        hotTrendValidation.createErrorBox("Lỗi", "Vui lòng chọn true hoặc false!");
        hotTrendValidation.setEmptyCellAllowed(false);
        sheet.addValidationData(hotTrendValidation);

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000); // Add padding
        }

        // Freeze header row
        sheet.createFreezePane(0, 1);

        workbook.write(response.getOutputStream());
        workbook.close();
    }




}
