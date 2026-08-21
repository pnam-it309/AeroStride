# AeroStride 🚀

> **Nền tảng thương mại điện tử giày & thời trang thể thao đa kênh (Web & Mobile App)**

[![Android APK](https://img.shields.io/badge/Download-APK%20v1.0.0-brightgreen?logo=android&logoColor=white&style=for-the-badge)](https://expo.dev/artifacts/eas/K4XBCJCrB7vS56cRfHTPAs2EepAqtcoNlFaX-KKJyJk.apk)
[![Expo Build](https://img.shields.io/badge/EAS%20Build-Preview%20Ready-blue?logo=expo&logoColor=white&style=for-the-badge)](https://expo.dev/accounts/pnamits-team/projects/aerostride/builds/7fdd67df-9619-40a0-b8c8-cc43297610ae)

---

## 📱 Tải ứng dụng AeroStride Mobile (Android)

Người dùng và nhà phát triển có thể tải trực tiếp file cài đặt APK để trải nghiệm trên thiết bị Android:

- 📥 **Link tải trực tiếp APK:** [AeroStride-v1.0.0.apk (101 MB)](https://expo.dev/artifacts/eas/K4XBCJCrB7vS56cRfHTPAs2EepAqtcoNlFaX-KKJyJk.apk)
- 🌐 **Xem chi tiết bản build trên Expo Cloud:** [Expo Build #7fdd67df](https://expo.dev/accounts/pnamits-team/projects/aerostride/builds/7fdd67df-9619-40a0-b8c8-cc43297610ae)
- 📲 **Cài đặt nhanh:** Quét mã QR hoặc mở link trên trình duyệt điện thoại Android để tải và cài đặt trực tiếp.

> 🔄 Link tải APK ở trên được **tự động cập nhật** sau mỗi lần build (CI/CD). Ứng dụng đã cài trên máy cũng sẽ **tự hiện thông báo cập nhật** khi có phiên bản mới.

---

## 🛠️ Cấu trúc dự án

```
AeroStride/
├── BE/               # Backend API (Java Spring Boot, MySQL, Redis, WebSocket)
├── FE/               # Web Frontend (Vue 3, Vuetify, Pinia, Vite)
├── mobile/           # Mobile App (React Native, Expo SDK 56, Expo Router)
├── docker/           # Docker Compose & triển khai hệ thống
├── docs/             # Tài liệu kiến trúc, sequence diagram & API specs
└── automation-tests/ # Kiểm thử tự động E2E
```

---

## 🚀 Khởi chạy nhanh

### 1. Web Frontend & Backend
```bash
# Khởi chạy toàn bộ hệ thống bằng Docker
docker compose up -d --build
```

- **Frontend:** http://localhost:5173
- **Backend Swagger / API:** http://localhost:8080

### 2. Mobile App (Expo)
```bash
cd mobile
npm install
npx expo start
```
