package org.fit.shopnuochoa.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.fit.shopnuochoa.model.OrderLine;
import org.fit.shopnuochoa.model.Orders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * ✅ FIX 1: validate email format chuẩn hơn để báo lỗi rõ ràng
     */
    public boolean isValidEmailFormat(String email) {
        try {
            InternetAddress mail = new InternetAddress(email);
            mail.validate();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * ✅ FIX 2: Gửi OTP với xử lý lỗi đầy đủ và return boolean
     */
    public boolean sendOtpEmail(String toEmail, String otpCode) {
        if (!isValidEmailFormat(toEmail)) {
            System.err.println("❌ Lỗi: Định dạng email không hợp lệ → " + toEmail);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("toannguyen041214@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Mã xác thực đăng ký tài khoản - SN Mobile");
            message.setText("Xin chào,\n\nMã OTP của bạn là: " + otpCode +
                    "\nMã này hết hạn sau 5 phút. Vui lòng không chia sẻ!");

            mailSender.send(message);
            System.out.println("📩 Đã gửi OTP thành công tới: " + toEmail);
            return true;

        } catch (MailException e) {
            System.err.println("❌ Gửi OTP thất bại tới " + toEmail + " → " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✅ FIX 3: Sinh OTP ngẫu nhiên 6 số
     */
    public String generateOtp() {
        int otp = 100000 + new Random().nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * ✅ FIX 4: Gửi hóa đơn kèm PDF, xử lý null tránh crash order.getCustomer()
     */
    public boolean sendInvoiceEmailWithPdf(Orders order) {
        try {
            if (order == null || order.getCustomer() == null) {
                System.err.println("❌ Lỗi: Order hoặc Customer NULL, không thể gửi email hóa đơn!");
                return false;
            }

            String toEmail = order.getCustomer().getEmail();
            if (!isValidEmailFormat(toEmail)) {
                System.err.println("❌ Email customer không hợp lệ: " + toEmail);
                return false;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("toannguyen041214@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("Hóa đơn mua hàng #" + order.getId());
            helper.setText("Cảm ơn bạn đã mua hàng. Hóa đơn chi tiết đính kèm bên dưới.", false);

            // 👉 Tạo PDF
            byte[] pdfBytes = buildPdfInvoice(order);
            helper.addAttachment("HoaDon_" + order.getId() + ".pdf",
                    new jakarta.mail.util.ByteArrayDataSource(pdfBytes, "application/pdf"));

            mailSender.send(message);
            System.out.println("✅ Gửi email hóa đơn thành công! → " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Gửi email hóa đơn thất bại → " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✅ FIX 5: Tách phần tạo PDF riêng → trả về byte[] tránh lỗi font & null
     */
    private byte[] buildPdfInvoice(Orders order) throws DocumentException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        generatePdfInvoice(order, stream);
        return stream.toByteArray();
    }

    /**
     * ✅ FIX 6: Vẽ PDF an toàn, tránh null, format tiền đúng chuẩn VN
     */
    private void generatePdfInvoice(Orders order, ByteArrayOutputStream outputStream) throws DocumentException {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        // Header
        document.add(new Paragraph("HÓA ĐƠN BÁN HÀNG",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22)));

        document.add(new Paragraph("Mã đơn hàng: #" + order.getId()));
        document.add(new Paragraph("Ngày tạo: " + order.getDate()));
        document.add(new Paragraph("Điện thoại: " + order.getPhoneNumber()));
        document.add(new Paragraph("Địa chỉ giao: " + order.getShippingAddress()));
        document.add(new Paragraph("Ghi chú: " + (order.getNote() != null ? order.getNote() : "Không có")));
        document.add(new Paragraph("\n"));

        // Table sản phẩm
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5f, 2f, 3f});

        table.addCell("Sản phẩm");
        table.addCell("Số lượng");
        table.addCell("Giá mua");

        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        if (order.getOrderLines() != null) {
            for (OrderLine line : order.getOrderLines()) {
                if (line.getProduct() != null) {
                    table.addCell(line.getProduct().getName());
                } else {
                    table.addCell("Unknown Product");
                }
                table.addCell(String.valueOf(line.getAmount()));
                table.addCell(currency.format(line.getPurchasePrice()));
            }
        }
        document.add(table);

        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Phí ship: " + currency.format(order.getShippingFee())));
        document.add(new Paragraph("Tổng thanh toán: " + currency.format(order.getFinalTotal()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));

        document.close();
    }
}
