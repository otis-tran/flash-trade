# 🚀 GitHub Actions Release Build Guide

Hướng dẫn chi tiết cách thiết lập và sử dụng GitHub Actions để tự động build APK release.

---

## 📋 Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Chuẩn bị secrets](#2-chuẩn-bị-secrets)
3. [Encode file sang Base64](#3-encode-file-sang-base64)
4. [Thêm secrets vào GitHub](#4-thêm-secrets-vào-github)
5. [Commit và push code](#5-commit-và-push-code)
6. [Tạo release](#6-tạo-release)
7. [Kiểm tra và download APK](#7-kiểm-tra-và-download-apk)

---

## 1. Tổng quan

Workflow tự động thực hiện:
- ✅ Build release APK với signing
- ✅ Upload APK lên GitHub Releases
- ✅ Tự động tạo release notes

**Trigger:**
- Push tag `v*` (ví dụ: `v1.0.0`)
- Manual trigger từ Actions tab

---

## 2. Chuẩn bị Secrets

### Danh sách secrets cần thiết:

| Secret Name | Mô tả | Cách lấy |
|------------|-------|----------|
| `PRIVY_APP_ID` | Privy App ID | [Privy Console](https://console.privy.io/) |
| `PRIVY_APP_CLIENT_ID` | Privy Client ID | [Privy Console](https://console.privy.io/) |
| `ETHERSCAN_API_KEY` | Etherscan API Key | [Etherscan APIs](https://etherscan.io/apis) |
| `ALCHEMY_API_KEY` | Alchemy API Key | [Alchemy Dashboard](https://www.alchemy.com/) |
| `KYBER_CLIENT_ID` | KyberSwap Client ID | Giá trị bất kỳ (ví dụ: `ftc-rin`) |
| `GOOGLE_SERVICES_JSON` | Firebase config (Base64) | [Xem hướng dẫn encode](#3-encode-file-sang-base64) |
| `KEYSTORE_BASE64` | Keystore file (Base64) | [Xem hướng dẫn encode](#3-encode-file-sang-base64) |
| `KEYSTORE_PASSWORD` | Keystore password | Password khi tạo keystore |
| `KEY_ALIAS` | Key alias | Alias khi tạo keystore |
| `KEY_PASSWORD` | Key password | Password của key |

---

## 3. Encode File sang Base64

### 3.1. Tạo Keystore (nếu chưa có)

```powershell
keytool -genkey -v -keystore flash-trade-release.keystore -alias flash-trade -keyalg RSA -keysize 2048 -validity 10000
```

Nhập các thông tin theo yêu cầu:
- **Keystore password**: Mật khẩu cho keystore
- **Key password**: Mật khẩu cho key (có thể giống keystore password)
- **Tên, tổ chức, địa chỉ**: Điền theo ý muốn

### 3.2. Encode Keystore sang Base64

**PowerShell:**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("flash-trade-release.keystore"))
```

**Hoặc lưu ra file:**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("flash-trade-release.keystore")) | Out-File -FilePath "keystore-base64.txt"
```

### 3.3. Encode google-services.json sang Base64

**PowerShell:**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app/google-services.json"))
```

**Hoặc lưu ra file:**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app/google-services.json")) | Out-File -FilePath "google-services-base64.txt"
```

### 3.4. Kiểm tra Base64 đã đúng chưa

**Decode thử:**
```powershell
# Decode keystore
[IO.File]::WriteAllBytes("test-keystore.keystore", [Convert]::FromBase64String((Get-Content "keystore-base64.txt")))

# Kiểm tra keystore
keytool -list -keystore test-keystore.keystore
```

---

## 4. Thêm Secrets vào GitHub

### Bước 1: Truy cập Settings

1. Vào repository trên GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**

### Bước 2: Thêm từng secret

Thêm 10 secrets với tên và giá trị tương ứng:

```
PRIVY_APP_ID          = <copy từ local.properties>
PRIVY_APP_CLIENT_ID   = <copy từ local.properties>
ETHERSCAN_API_KEY     = <copy từ local.properties>
ALCHEMY_API_KEY       = <copy từ local.properties>
KYBER_CLIENT_ID       = <copy từ local.properties>
GOOGLE_SERVICES_JSON  = <chuỗi base64 của google-services.json>
KEYSTORE_BASE64       = <chuỗi base64 của keystore>
KEYSTORE_PASSWORD     = <mật khẩu keystore>
KEY_ALIAS             = flash-trade
KEY_PASSWORD          = <mật khẩu key>
```

> ⚠️ **Lưu ý:** Paste trực tiếp chuỗi Base64, không có dấu xuống dòng thừa.

---

## 5. Commit và Push Code

### 5.1. Commit code lên nhánh dev

```powershell
git add .
git commit -m "feat: add GitHub Actions release workflow"
git push origin dev
```

### 5.2. Tạo Pull Request và Merge vào main

1. Vào GitHub → **Pull requests** → **New pull request**
2. Base: `main` ← Compare: `dev`
3. Click **Create pull request**
4. Review và **Merge pull request**

### 5.3. Checkout main và pull code mới

```powershell
git checkout main
git pull origin main
```

---

## 6. Tạo Release

### Option 1: Tạo tag để trigger workflow tự động

```powershell
# Tạo tag
git tag -a v1.0.0 -m "Release v1.0.0 - Flash Trade MVP"

# Push tag lên GitHub
git push origin v1.0.0
```

### Option 2: Chạy workflow thủ công

1. Vào GitHub → **Actions** → **Build Release APK**
2. Click **Run workflow**
3. Chọn branch và click **Run workflow**

---

## 7. Kiểm tra và Download APK

### 7.1. Xem tiến trình build

1. Vào **Actions** tab
2. Click vào workflow run mới nhất
3. Xem logs từng step

### 7.2. Download APK

**Nếu build thành công:**

1. Vào **Releases** tab (https://github.com/YOUR_USERNAME/flash-trade/releases)
2. Click vào release mới nhất
3. Download file `flash-trade.apk`

**Hoặc từ Artifacts:**

1. Vào workflow run
2. Scroll xuống phần **Artifacts**
3. Download `flash-trade-v1.0.0`

---

## 🔧 Troubleshooting

### Lỗi: "No key with alias 'flash-trade' found"

**Nguyên nhân:** KEY_ALIAS không khớp với alias trong keystore.

**Giải pháp:**
```powershell
# Xem alias trong keystore
keytool -list -keystore flash-trade-release.keystore
```
Cập nhật secret `KEY_ALIAS` cho đúng.

### Lỗi: "google-services.json not found"

**Nguyên nhân:** Base64 không đúng hoặc chưa thêm secret.

**Giải pháp:** Encode lại và kiểm tra decode thử trước khi thêm vào secrets.

### Lỗi: Build failed with "Could not resolve..."

**Nguyên nhân:** Vấn đề về dependencies.

**Giải pháp:** Build local trước để đảm bảo code không có lỗi:
```powershell
./gradlew assembleRelease
```

---

## 📁 Cấu trúc Files

```
flash-trade/
├── .github/
│   └── workflows/
│       └── release.yml          # GitHub Actions workflow
├── app/
│   ├── build.gradle.kts         # Cấu hình build với signing
│   └── google-services.json     # (gitignored) Firebase config
├── local.properties             # (gitignored) API keys, keystore config
├── local.properties.example     # Template cho local.properties
└── flash-trade-release.keystore # (gitignored) Release keystore
```

---

## ✅ Checklist trước khi Release

- [ ] Đã thêm đủ 10 secrets trên GitHub
- [ ] Đã encode đúng google-services.json và keystore
- [ ] Đã merge code vào main
- [ ] Đã test build local thành công
- [ ] Đã tạo tag version đúng format `v*`
