# Firebase Crashlytics Setup Guide

## Step 1: Tạo Firebase Project

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"**
3. Đặt tên project: `Flash Trade`
4. Disable Google Analytics (optional - đã có trong code)
5. Click **"Create project"**

## Step 2: Thêm Android App

1. Trong Firebase Console, click icon **Android** để thêm app
2. Điền thông tin:
   - **Package name:** `com.otistran.flash_trade`
   - **App nickname:** Flash Trade
   - **SHA-1:** (optional, cho Google Sign-In)
3. Click **"Register app"**

## Step 3: Download google-services.json

1. Download file `google-services.json`
2. Copy vào folder này:
   ```
   d:\projects\flash-trade\app\google-services.json
   ```

## Step 4: Build và Test

```powershell
.\gradlew assembleRelease
```

## Step 5: Kiểm tra Crashlytics Dashboard

1. Install APK lên device
2. Mở app (nếu crash, crash sẽ được gửi)
3. Mở [Firebase Console](https://console.firebase.google.com/) → Crashlytics
4. Sau vài phút, crash reports sẽ xuất hiện

## Force Test Crash (Optional)

Thêm code này vào bất kỳ button nào để test:

```kotlin
throw RuntimeException("Test Crash for Crashlytics")
```

## Notes

- Crash reports gửi khi app khởi động lại sau crash
- Đầu tiên có thể mất 5-10 phút để xuất hiện
- Release builds có obfuscated stack traces (cần upload mapping.txt)

## Upload Mapping File (for Release)

Crashlytics plugin tự động upload mapping file khi build release.
