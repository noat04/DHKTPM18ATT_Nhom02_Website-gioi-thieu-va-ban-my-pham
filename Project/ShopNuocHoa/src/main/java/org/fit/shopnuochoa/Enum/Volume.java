package org.fit.shopnuochoa.Enum;

public enum Volume {

    // Định nghĩa các hằng số enum, gọi constructor với giá trị ml tương ứng
    ML_10(10),
    ML_30(30),
    ML_50(50),
    ML_75(75),
    ML_100(100),
    ML_150(150),
    ML_200(200);

    // 1. Thêm một trường (field) private để lưu giá trị số (ml)
    private final int valueInMl;

    // 2. Tạo một constructor (private) để nhận giá trị này
    Volume(int valueInMl) {
        this.valueInMl = valueInMl;
    }

    // 3. Tạo một getter để các lớp khác có thể truy xuất giá trị số
    public int getValueInMl() {
        return valueInMl;
    }

    // 4. Ghi đè phương thức toString() để hiển thị tên đẹp hơn
    @Override
    public String toString() {
        return this.valueInMl + "ml";
    }
}