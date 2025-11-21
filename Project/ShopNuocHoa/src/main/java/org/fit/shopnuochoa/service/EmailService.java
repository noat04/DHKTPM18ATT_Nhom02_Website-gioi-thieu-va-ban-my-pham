package org.fit.shopnuochoa.service;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.regex.Pattern;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Regex đơn giản để kiểm tra định dạng email
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    /**
     * Gửi OTP và kiểm tra lỗi
     * @return true nếu gửi thành công, false nếu có lỗi
     */
    public boolean sendOtpEmail(String toEmail, String otpCode) {
        // 1. Kiểm tra định dạng email trước
        if (!isValidEmailFormat(toEmail)) {
            System.err.println("Lỗi: Định dạng email không hợp lệ: " + toEmail);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("toannguyen041214@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Mã xác thực đăng ký tài khoản - SN Mobile");
            message.setText("Xin chào,\n\nMã xác thực (OTP) của bạn là: " + otpCode +
                    "\n\nMã này sẽ hết hạn sau 5 phút.\nVui lòng không chia sẻ mã này cho bất kỳ ai.");

            // 2. Thực hiện gửi
            mailSender.send(message);
            System.out.println("Đã gửi OTP đến: " + toEmail);
            return true;

        } catch (MailException e) {
            // 3. Bắt lỗi nếu gửi thất bại (Ví dụ: Email không tồn tại, Lỗi mạng, Lỗi server)
            e.printStackTrace();
            System.err.println("Gửi mail thất bại tới " + toEmail + ": " + e.getMessage());
            return false;
        }
    }

    // Hàm sinh mã OTP ngẫu nhiên (6 số)
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Hàm phụ trợ kiểm tra định dạng Email
     */
    public boolean isValidEmailFormat(String email) {
        if (email == null || email.isEmpty()) return false;

        // Cách 2: Dùng thư viện Java Mail (Chính xác hơn)
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
            return true;
        } catch (AddressException ex) {
            return false;
        }
    }
}