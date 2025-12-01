package org.fit.shopnuochoa.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.fit.shopnuochoa.model.Customer;
import org.fit.shopnuochoa.model.Users;
import org.fit.shopnuochoa.service.CloudinaryService;
import org.fit.shopnuochoa.service.CustomerService;
import org.fit.shopnuochoa.service.EmailService;
import org.fit.shopnuochoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/api")
@SessionAttributes("loggedInUser")
public class UserController {

    private final EmailService emailService;
    private final UserService userService;
    private final CustomerService customerService;
    private final CloudinaryService cloudinaryService;
    @Autowired
    public UserController(UserService userService,
                          CustomerService customerService,
                          CloudinaryService cloudinaryService,
                          EmailService emailService) {
        this.userService = userService;
        this.customerService = customerService;
        this.cloudinaryService= cloudinaryService;
        this.emailService=emailService;
    }

    // Cung cấp một đối tượng trống cho các form binding
    @ModelAttribute("userForm")
    public Users userForm() {
        return new Users();
    }

    /**
     * HIỂN THỊ TRANG ĐĂNG NHẬP
     * Xử lý các tín hiệu từ Spring Security (?error và ?logout)
     */
    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {

        // Kiểm tra lỗi đăng nhập (Spring Security trả về)
        if (error != null) {
            model.addAttribute("errorMessage", "Tài khoản hoặc mật khẩu không chính xác!");
        }

        // Kiểm tra đăng xuất
        if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất thành công.");
        }

        return "screen/login";
    }

    // PHƯƠNG THỨC @PostMapping("/login") ĐÃ ĐƯỢC XÓA
    // VÌ SPRING SECURITY TỰ ĐỘNG XỬ LÝ VIỆC NÀY

    // Xử lý đăng xuất (giữ nguyên)
    @GetMapping("/logout")
    public String logout(HttpSession session, SessionStatus status) {
        status.setComplete();
        session.invalidate();
        return "redirect:/api/login?logout";
    }

    // Các phương thức đăng ký
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registerForm", new Users());
        return "screen/customer/register";
    }

    // Xử lý form đăng ký -> Gửi OTP
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registerForm") Users user,
                                      BindingResult bindingResult, // Quan trọng: Phải nằm ngay sau @ModelAttribute
                                      HttpSession session,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {

        // Kiểm tra Username trùng
        if (userService.getUserByUsername(user.getUsername()) != null) {
            // "username": tên trường trong Entity
            // "error.user": mã lỗi (tùy chọn)
            // "Tên đăng nhập...": thông báo hiển thị
            bindingResult.rejectValue("username", "error.user", "Tên đăng nhập đã tồn tại!");
        }

        // Kiểm tra Email trùng (Sử dụng hàm bạn đã có)
        // Lưu ý: Cần đảm bảo userService có hàm findByEmail trả về Optional
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            bindingResult.rejectValue("email", "error.user", "Email này đã được sử dụng!");
        }

        // --- 2. KIỂM TRA TỔNG HỢP ---
        // (Bao gồm cả lỗi @Valid như để trống VÀ lỗi trùng lặp vừa thêm ở trên)
        if (bindingResult.hasErrors()) {
            return "screen/customer/register"; // Trả về form, Thymeleaf sẽ tự hiển thị lỗi dưới từng ô input
        }

        try {
            // --- 3. GỬI OTP (Khi dữ liệu đã hợp lệ) ---
            String otp = emailService.generateOtp();
            boolean isSent = emailService.sendOtpEmail(user.getEmail(), otp);

            if (!isSent) {
                // Lỗi gửi mail là lỗi hệ thống, nên dùng model.addAttribute (hiển thị alert chung)
                model.addAttribute("errorMessage", "Không thể gửi mã xác thực. Vui lòng kiểm tra lại địa chỉ Email!");
                return "screen/customer/register";
            }

            // --- 4. LƯU SESSION & CHUYỂN HƯỚNG ---
            session.setAttribute("tempUser", user);
            session.setAttribute("otpCode", otp);
            session.setAttribute("otpTime", System.currentTimeMillis());

            return "redirect:/api/verify-otp";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "screen/customer/register";
        }
    }

    // Hiển thị trang nhập OTP
    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("tempUser") == null) {
            ra.addFlashAttribute("errorMessage", "Phiên đăng ký đã hết hạn.");
            return "redirect:/api/register";
        }
        return "screen/customer/verify-otp";
    }

    // Xử lý xác thực OTP
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") String inputOtp,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        String sessionOtp = (String) session.getAttribute("otpCode");
        Long otpTime = (Long) session.getAttribute("otpTime");
        Users tempUser = (Users) session.getAttribute("tempUser");

        // Kiểm tra session
        if (sessionOtp == null || tempUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc hết hạn. Vui lòng đăng ký lại.");
            return "redirect:/api/register";
        }

        // Kiểm tra thời gian (Ví dụ: hết hạn sau 5 phút = 300000ms)
        if (System.currentTimeMillis() - otpTime > 5 * 60 * 1000) {
            session.removeAttribute("otpCode"); // Xóa OTP cũ
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP đã hết hạn!");
            return "redirect:/api/register"; // Hoặc cho phép gửi lại OTP
        }

        // Kiểm tra mã OTP
        if (inputOtp.equals(sessionOtp)) {
            // === OTP ĐÚNG -> LƯU VÀO DB ===
            try {
                // 1. Lưu User
                Users savedUser = userService.registerNewUser(tempUser);

                // 2. Tạo Customer
                Customer newCustomer = new Customer();
                newCustomer.setName(savedUser.getFull_name());
                customerService.createCustomer(newCustomer, savedUser.getId());

                // 3. Xóa session tạm
                session.removeAttribute("tempUser");
                session.removeAttribute("otpCode");
                session.removeAttribute("otpTime");

                redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
                return "redirect:/api/login";

            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi lưu dữ liệu: " + e.getMessage());
                return "redirect:/api/register";
            }
        } else {
            // OTP SAI
            redirectAttributes.addFlashAttribute("errorMessage", "Mã OTP không chính xác. Vui lòng thử lại.");
            return "redirect:/api/verify-otp";
        }
    }

    // 1. Hiển thị trang nhập Email
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "screen/forgot-password";
    }

    // 2. Xử lý gửi OTP về Email
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        HttpSession session,
                                        RedirectAttributes ra) {
        // Kiểm tra email có tồn tại trong DB không
        // (Giả sử bạn đã có hàm findByEmail trong UserService/Repo trả về Optional)
        // Hoặc dùng try-catch nếu hàm của bạn ném lỗi
        try {
            Optional<Users> user = userService.findByEmail(email); // Cần đảm bảo hàm này có trong Service
            if (user == null) {
                ra.addFlashAttribute("errorMessage", "Email này chưa được đăng ký!");
                return "redirect:/api/forgot-password";
            }

            // Sinh OTP và gửi mail
            String otp = emailService.generateOtp();
            boolean isSent = emailService.sendOtpEmail(email, otp);

            if (!isSent) {
                ra.addFlashAttribute("errorMessage", "Lỗi gửi mail. Vui lòng thử lại sau.");
                return "redirect:/api/forgot-password";
            }

            // Lưu thông tin vào Session để sang bước sau check
            session.setAttribute("resetEmail", email);
            session.setAttribute("resetOtp", otp);
            session.setAttribute("resetTime", System.currentTimeMillis());

            return "redirect:/api/reset-password";

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/api/forgot-password";
        }
    }

    // 3. Hiển thị trang Nhập OTP và Mật khẩu mới
    // Trong ForgotPasswordController.java

    @GetMapping("/reset-password")
    public String showResetPasswordForm(HttpSession session, Model model, RedirectAttributes ra) {
        // 1. Kiểm tra session xem có hợp lệ không
        Long resetTime = (Long) session.getAttribute("resetTime");
        if (session.getAttribute("resetEmail") == null || resetTime == null) {
            ra.addFlashAttribute("errorMessage", "Hết phiên làm việc. Vui lòng thực hiện lại.");
            return "redirect:/api/forgot-password";
        }

        // 2. [THÊM MỚI] Tính thời gian còn lại (theo giây)
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - resetTime; // Thời gian đã trôi qua (ms)
        long timeLimit = 5 * 60 * 1000; // Giới hạn 5 phút (ms)

        long remainingMillis = timeLimit - timeElapsed;
        long remainingSeconds = remainingMillis / 1000;

        // Nếu đã hết giờ (số âm), gán về 0
        if (remainingSeconds < 0) {
            remainingSeconds = 0;
        }

        // 3. Gửi số giây còn lại sang View
        model.addAttribute("remainingSeconds", remainingSeconds);

        return "screen/reset-password";
    }

    // 4. Xử lý Đổi mật khẩu
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("otp") String otp,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       HttpSession session,
                                       RedirectAttributes ra) {

        String sessionOtp = (String) session.getAttribute("resetOtp");
        String email = (String) session.getAttribute("resetEmail");
        Long otpTime = (Long) session.getAttribute("resetTime");

        if (email == null || sessionOtp == null) {
            ra.addFlashAttribute("errorMessage", "Yêu cầu không hợp lệ.");
            return "redirect:/api/forgot-password";
        }

        // Check hết hạn (5 phút)
        if (System.currentTimeMillis() - otpTime > 5 * 60 * 1000) {
            ra.addFlashAttribute("errorMessage", "Mã OTP đã hết hạn.");
            return "redirect:/api/forgot-password";
        }

        // Check OTP
        if (!otp.equals(sessionOtp)) {
            ra.addFlashAttribute("errorMessage", "Mã OTP không chính xác.");
            return "redirect:/api/reset-password";
        }

        // Check mật khẩu trùng khớp
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp.");
            return "redirect:/api/reset-password";
        }

        // Thực hiện đổi mật khẩu
        userService.updatePassword(email, newPassword);

        // Xóa session
        session.removeAttribute("resetEmail");
        session.removeAttribute("resetOtp");
        session.removeAttribute("resetTime");

        ra.addFlashAttribute("successMessage", "Đổi mật khẩu thành công! Vui lòng đăng nhập.");
        return "redirect:/api/login";
    }

    // UPLOAD AVATAR LÊN CLOUDINARY
    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        // 1. Kiểm tra file rỗng
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file ảnh!");
            return "redirect:/api/profile";
        }

        try {
            // 2. Lấy User đang đăng nhập
            String username = principal.getName();

            // Lưu ý: Trong UserService của bạn, hàm tìm user trả về trực tiếp Users (có thể null)
            // Nên kiểm tra null để tránh lỗi
            Users currentUser = userService.getUserByUsername(username);
            if (currentUser == null) {
                throw new RuntimeException("Không tìm thấy người dùng!");
            }

            // 3. Gọi Service upload lên Cloudinary
            String avatarUrl = cloudinaryService.uploadAvatar(file);

            // 4. Set Avatar mới
            currentUser.setAvatar(avatarUrl);

            // 5. Gọi hàm saveUser vừa thêm
            userService.save(currentUser);

            redirectAttributes.addFlashAttribute("successMessage", "Đổi ảnh đại diện thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
        }

        return "redirect:/api/profile";
    }

    /**
     * Xử lý xóa ảnh đại diện
     * URL: POST /api/delete-avatar
     */
    @PostMapping("/delete-avatar")
    public String deleteAvatar(Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            if (principal == null) {
                return "redirect:/api/login";
            }

            String username = principal.getName();
            Users currentUser = userService.getUserByUsername(username);

            if (currentUser == null) {
                throw new RuntimeException("User không tồn tại");
            }

            String currentAvatarUrl = currentUser.getAvatar();

            // 1. Kiểm tra xem có phải ảnh mặc định không
            if (currentAvatarUrl != null && currentAvatarUrl.startsWith("http")) {

                // 2. Xóa trên Cloudinary (Nên bọc try-catch riêng để không chết app nếu lỗi mạng)
                try {
                    cloudinaryService.deleteImageByUrl(currentAvatarUrl);
                } catch (Exception e) {
                    System.err.println("Lỗi khi xóa ảnh trên Cloudinary (có thể bỏ qua): " + e.getMessage());
                }

                // 3. Xóa trong Database
                currentUser.setAvatar(null);
                userService.save(currentUser);

                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ảnh đại diện!");
            } else {
                // Nếu avatar là null hoặc ảnh local
                currentUser.setAvatar(null); // Đảm bảo về null
                userService.save(currentUser);
                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ảnh đại diện!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa ảnh: " + e.getMessage());
        }

        return "redirect:/api/profile";
    }


    // Hiển thị trang hồ sơ (Bao gồm cả Tab Xem và Tab Sửa)
    @GetMapping("/profile")
    public String viewProfile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/api/login";
        }

        String username = principal.getName();
        Users user = userService.getUserByUsername(username);

        // Tìm thông tin khách hàng chi tiết
        Customer customer = customerService.getByUser(user.getId());

        // Nếu chưa có thông tin customer (lần đầu), tạo mới
        if (customer == null) {
            customer = new Customer();
            customer.setUser(user);
            customer.setName(user.getFull_name());
            customer.setEmail(user.getEmail());
            // Lưu tạm để có ID (nếu cần) hoặc để binding form
        }

        model.addAttribute("user", user);
        model.addAttribute("customer", customer);

        return "screen/customer/account-setting"; // Trả về file profile.html (có tabs)
    }

    // Xử lý cập nhật hồ sơ
    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute("customer") Customer updatedCustomer, // [1] Kích hoạt Validate
                                BindingResult result, // [2] Chứa kết quả lỗi
                                Principal principal,
                                RedirectAttributes redirectAttributes,
                                Model model) { // Cần Model để đẩy dữ liệu khi có lỗi

        if (principal == null) {
            return "redirect:/api/login";
        }

        // Lấy thông tin User hiện tại (cần dùng cho cả trường hợp thành công và thất bại)
        String username = principal.getName();
        Users user = userService.getUserByUsername(username);

        // 1. Kiểm tra lỗi Validation (SĐT sai, Tên trống...)
        if (result.hasErrors()) {
            // [QUAN TRỌNG] Khi có lỗi, trả về trang cũ chứ không redirect
            // Để giữ lại thông báo lỗi và dữ liệu người dùng vừa nhập
            model.addAttribute("user", user);
            // Cần set lại User cho customer để hiển thị avatar/email (nếu giao diện cần)
            updatedCustomer.setUser(user);

            // Nếu giao diện profile cần danh sách đơn hàng hay gì khác, hãy load lại ở đây
            // model.addAttribute("orders", ...);

            return "screen/customer/account-setting"; // Tên file HTML trang cá nhân của bạn
        }

        try {
            // 2. Xử lý logic cập nhật
            Customer existingCustomer = customerService.getByUser(user.getId());

            // Gọi hàm update (lưu ý: chỉ copy các field cho phép sửa)
            customerService.updateCustomer(existingCustomer.getId(), updatedCustomer);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
            return "redirect:/api/profile"; // Thành công thì Redirect để refresh

        } catch (Exception e) {
            e.printStackTrace();
            // Lỗi hệ thống -> Trả về view để báo lỗi
            model.addAttribute("user", user);
            model.addAttribute("errorMessage", "Lỗi cập nhật: " + e.getMessage());
            updatedCustomer.setUser(user); // Re-bind user
            return "screen/customer/account-setting";
        }
    }


    // ==========================
    // 🔹 QUẢN LÝ TÀI KHOẢN (ADMIN)
    // ==========================
    @GetMapping("/admin/users")
    public String listAllUsers(Model model) {
        model.addAttribute("users", userService.getAll());
        return "screen/admin/admin-user-list";
    }

    // Xem chi tiết tài khoản (không hiển thị mật khẩu)
    @GetMapping("/admin/users/{id}")
    public String viewUserDetails(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        Users user = userService.getUserById(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
            return "redirect:/api/admin/users";
        }
        model.addAttribute("user", user);
        return "screen/admin/admin-user-detail";
    }

    // Xóa tài khoản (chỉ khi chưa từng đặt hàng)
    @PostMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable int id, RedirectAttributes ra) {
        try {
            boolean deleted = userService.deleteUserById(id);
            if (deleted) {
                ra.addFlashAttribute("successMessage", "Xóa tài khoản thành công.");
            } else {
                ra.addFlashAttribute("errorMessage", "Tài khoản không tồn tại.");
            }
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/api/admin/users";
    }

    // Hiển thị form chỉnh sửa người dùng
    @GetMapping("/admin/users/update/{id}")
    public String showEditForm(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        Users user = userService.getUserById(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng!");
            return "redirect:/api/admin/users";
        }
        model.addAttribute("user", user);
        return "screen/admin/admin-user-edit"; // View đúng
    }

    // Cập nhật thông tin người dùng
    @PostMapping("/admin/users/update/{id}")
    public String updateUser(@PathVariable int id,
                             @ModelAttribute("user") Users updatedUser,
                             RedirectAttributes redirectAttributes) {
        Optional<Users> updated = userService.updateUser(id, updatedUser);
        if (updated.isPresent()) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật tài khoản!");
        }
        return "redirect:/api/admin/users";
    }
}