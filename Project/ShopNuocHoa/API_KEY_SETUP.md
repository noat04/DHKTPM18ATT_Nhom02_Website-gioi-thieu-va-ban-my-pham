# 🔐 HƯỚNG DẪN CÀI ĐẶT GEMINI API KEY

Hướng dẫn đơn giản để lấy API key từ Google AI Studio và cấu hình biến môi trường.

---

## Bước 1: Lấy API Key

1. Truy cập: https://aistudio.google.com/app/apikey
2. Đăng nhập bằng tài khoản Google
3. Nhấn **"Create API Key"** → Chọn project
4. **Copy key ngay** (chỉ hiển thị 1 lần)
5. Key có dạng: `AIzaSy...`

---

## Bước 2: Cài đặt biến môi trường

### Windows - Cách 1: Giao diện (GUI)

1. Nhấn `Windows + R` → gõ `sysdm.cpl` → Enter
2. Tab **Advanced** → **Environment Variables**
3. Phần **User variables** → **New**
   - Variable name: `GEMINI_API_KEY`
   - Variable value: `<paste key ở đây>`
4. Nhấn **OK** → **OK**
5. **Khởi động lại IntelliJ IDEA**

### Windows - Cách 2: PowerShell (nhanh)

Mở PowerShell và chạy:

```powershell
# Lưu vĩnh viễn cho user hiện tại
[System.Environment]::SetEnvironmentVariable('GEMINI_API_KEY','AIzaSy_YOUR_KEY_HERE','User')
```

Sau đó **khởi động lại IntelliJ IDEA**.

### Linux / macOS

```bash
# Thêm vào file ~/.bashrc hoặc ~/.zshrc
echo "export GEMINI_API_KEY='AIzaSy_YOUR_KEY_HERE'" >> ~/.bashrc

# Load lại
source ~/.bashrc
```

---

## Bước 3: Kiểm tra

Mở terminal mới và kiểm tra:

**Windows PowerShell:**
```powershell
echo $Env:GEMINI_API_KEY
```

**Windows CMD:**
```cmd
echo %GEMINI_API_KEY%
```

**Linux/macOS:**
```bash
echo $GEMINI_API_KEY
```

Nếu hiển thị key → **Thành công!**  
Nếu trống → Khởi động lại terminal hoặc IDE.

---
