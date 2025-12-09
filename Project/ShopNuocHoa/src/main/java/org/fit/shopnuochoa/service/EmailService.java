package org.fit.shopnuochoa.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
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
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

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

        // ========= HEADER =========
        Paragraph header = new Paragraph("HÓA DON BÁN HÀNG",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24));
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);

        Paragraph shopName = new Paragraph("SHOP NUOC HOA TDDN",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        shopName.setAlignment(Element.ALIGN_CENTER);
        document.add(shopName);

        Paragraph shopInfo = new Paragraph("Hotline: 0123 456 789  |  Email: contact@ttdn.vn",
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        shopInfo.setAlignment(Element.ALIGN_CENTER);
        document.add(shopInfo);

        document.add(new Paragraph("\n"));

        // ========= THÔNG TIN ĐƠN HÀNG =========
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(10);

        infoTable.setWidths(new float[]{3f, 7f});

        infoTable.addCell("Mã đơn hàng:");
        infoTable.addCell("#" + order.getId());

        infoTable.addCell("Ngày tạo:");
        infoTable.addCell(String.valueOf(order.getDate()));

        infoTable.addCell("Tên khách hàng:");
        infoTable.addCell(order.getCustomer().getName() != null ? order.getCustomer().getName() : "Không có");

        infoTable.addCell("Số điện thoại:");
        infoTable.addCell(order.getPhoneNumber());

        infoTable.addCell("Địa chỉ giao:");
        infoTable.addCell(order.getShippingAddress());

        infoTable.addCell("Ghi chú:");
        infoTable.addCell(order.getNote() != null ? order.getNote() : "Không có");

        document.add(infoTable);

        // ========= BẢNG SẢN PHẨM =========
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5f, 2f, 3f, 3f});
        table.setSpacingBefore(10);

        // Header
        Stream.of("Sản phẩm", "SL", "Giá mua", "Thành tiền")
                .forEach(headerTitle -> {
                    PdfPCell cell = new PdfPCell(new Phrase(headerTitle,
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//                    cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    table.addCell(cell);
                });

        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        BigDecimal total = BigDecimal.ZERO;

        if (order.getOrderLines() != null) {
            for (OrderLine line : order.getOrderLines()) {

                String productName = (line.getProduct() != null)
                        ? line.getProduct().getName()
                        : "Unknown Product";

                BigDecimal price = line.getPurchasePrice();
                BigDecimal quantity = BigDecimal.valueOf(line.getAmount());
                BigDecimal lineTotal = price.multiply(quantity);

                total = total.add(lineTotal);

                table.addCell(productName);
                table.addCell(String.valueOf(line.getAmount()));
                table.addCell(currency.format(price));
                table.addCell(currency.format(lineTotal));
            }
        }


        document.add(table);

        document.add(new Paragraph("\n"));

        // ========= TỔNG TIỀN =========
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(50);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        totalTable.addCell("Tổng giá trị sản phẩm:");
        totalTable.addCell(currency.format(total));

        totalTable.addCell("Phí ship:");
        totalTable.addCell(currency.format(order.getShippingFee()));

        if (order.getDiscountAmount() != null) {
            totalTable.addCell("Giảm giá:");
            totalTable.addCell("-" + currency.format(order.getDiscountAmount()));
        }

        totalTable.addCell("Tổng thanh toán:");
        totalTable.addCell(new Phrase(
                currency.format(order.getFinalTotal()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)
        ));

        document.add(totalTable);

        document.add(new Paragraph("\n\nCảm ơn bạn đã mua hàng tại TDDN!"));
        document.add(new Paragraph("Hẹn gặp bạn lần sau ♥"));

        document.close();
    }

}
