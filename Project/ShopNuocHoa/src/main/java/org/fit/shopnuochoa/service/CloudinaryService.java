package org.fit.shopnuochoa.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload ảnh Coupon
     */
    public String uploadCouponImage(MultipartFile file) {
        return uploadFile(file, "shopnuochoa_coupons");
    }

    /**
     * Cập nhật ảnh Coupon (Xóa cũ -> Up mới)
     */
    public String updateCouponImage(MultipartFile newFile, String oldImageUrl) {
        // 1. Xóa ảnh cũ nếu có
        deleteImageIfExist(oldImageUrl);
        // 2. Upload ảnh mới
        return uploadCouponImage(newFile);
    }

    // ==================================================
    // XỬ LÝ AVATAR (USER)
    // ==================================================

    public String uploadAvatar(MultipartFile file) {
        return uploadFile(file, "shopnuochoa_avatars");
    }

    public String updateAvatar(MultipartFile newFile, String oldAvatarUrl) {
        // 1. Xóa ảnh cũ nếu có
        deleteImageIfExist(oldAvatarUrl);
        // 2. Upload ảnh mới
        return uploadAvatar(newFile);
    }

    // ==================================================
    // XỬ LÝ ẢNH SẢN PHẨM (PRODUCT) - MỚI THÊM
    // ==================================================

    /**
     * Upload 1 ảnh sản phẩm
     */
    public String uploadProductImage(MultipartFile file) {
        return uploadFile(file, "shopnuochoa_products");
    }

    /**
     * Cập nhật ảnh sản phẩm (Xóa ảnh cũ -> Up ảnh mới)
     */
    public String updateProductImage(MultipartFile newFile, String oldImageUrl) {
        // 1. Xóa ảnh cũ nếu có
        deleteImageIfExist(oldImageUrl);
        // 2. Upload ảnh mới
        return uploadProductImage(newFile);
    }

    /**
     * Upload nhiều ảnh sản phẩm cùng lúc
     * @return Danh sách các URL ảnh đã upload
     */
    public List<String> uploadMultipleProductImages(MultipartFile[] files) {
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                imageUrls.add(uploadProductImage(file));
            }
        }
        return imageUrls;
    }

    // ==================================================
    // ⚙️ PHẦN 3: CÁC HÀM CHUNG (COMMON)
    // ==================================================

    /**
     * Hàm upload chung cho cả Avatar và Product
     * @param folderName Tên thư mục trên Cloudinary
     */
    private String uploadFile(MultipartFile file, String folderName) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folderName,
                            "resource_type", "image"
                    ));
            return uploadResult.get("url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
        }
    }

    /**
     * Xóa ảnh trên Cloudinary dựa vào URL
     */
    public void deleteImageByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        try {
            String publicId = getPublicIdFromUrl(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xóa ảnh trên Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Hàm tiện ích: Kiểm tra và xóa ảnh cũ nếu đó là ảnh trên Cloudinary
     */
    private void deleteImageIfExist(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("http")) {
            // Kiểm tra thêm điều kiện để tránh xóa ảnh mặc định hoặc ảnh placeholder nếu có
            deleteImageByUrl(imageUrl);
        }
    }

    /**
     * Trích xuất Public ID từ URL
     */
    private String getPublicIdFromUrl(String url) {
        try {
            int uploadIndex = url.indexOf("upload/");
            if (uploadIndex == -1) return null;

            String path = url.substring(uploadIndex + 7);

            if (path.startsWith("v")) {
                int slashIndex = path.indexOf("/");
                if (slashIndex != -1) {
                    path = path.substring(slashIndex + 1);
                }
            }

            int dotIndex = path.lastIndexOf(".");
            if (dotIndex != -1) {
                return path.substring(0, dotIndex);
            }
            return path;
        } catch (Exception e) {
            return null;
        }
    }
}