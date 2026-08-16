# Cấu trúc Dự án (Project Architecture) - Backend

Dự án này là hệ thống Backend phục vụ cho **Hệ thống Cứu hộ thảm họa (Disaster Rescue)**, được xây dựng theo kiến trúc Microservices với Java/Spring Boot.

## 1. Cấu trúc thư mục (Directory Structure)

Dự án được chia thành hai nhóm chính: `core-service` (chứa các business microservices) và `infrastructure` (chứa các dịch vụ hạ tầng).

- `core-service/`: Các service nghiệp vụ lõi.
  - `common-lib/`: Thư viện dùng chung (global exceptions, api response format).
  - `dispatch-service/`: Service điều phối và phân công (quản lý Hazard, Rescue Request, Assignment, MapPoint).
  - `integration-service/`: Service tích hợp bên ngoài (chứa logic SocialRawFeed).
  - `intergation-ai-service/`: Service tích hợp AI xử lý thông tin thảm họa.
  - `notification-service/`: Service thông báo (quản lý TeamLocationLog).
  - `resource-management-service/`: Service quản lý tài nguyên và chiến dịch (Campaign, Item, RescueTeam, Inventory).
  - `user-service/`: Service quản lý người dùng, hồ sơ và xác thực (User, VolunteerProfile, Auth).
- `infrastructure/`: Các thành phần hạ tầng hệ thống.
  - `api-gateway/`: Cổng giao tiếp API Gateway.
  - `config-service/`: Cấu hình tập trung (Spring Cloud Config Service) chứa file cấu hình cho tất cả các service khác.
  - `eureka-server/`: Service Discovery (Spring Cloud Netflix Eureka).

## 2. Tổng hợp API (API Endpoints)

Hiện tại, các API đã được triển khai chủ yếu tập trung tại module `user-service`. (Các module khác như `dispatch-service`, `resource-management-service` đã có cấu trúc Entities, Service, Repository nhưng chưa định nghĩa RestController).

### A. User Service (`core-service/user-service`)

**1. Auth Controller (`/api/v1/auth`)**
- `POST /api/v1/auth/register/citizen` : Đăng ký tài khoản cho Người dân.
- `POST /api/v1/auth/register/rescuer` : Đăng ký tài khoản cho Tình nguyện viên / Đội cứu hộ.
- `POST /api/v1/auth/verify-otp` : Xác thực mã OTP.
- `POST /api/v1/auth/login` : Đăng nhập hệ thống.
- `POST /api/v1/auth/refresh-token` : Làm mới Access Token.
- `POST /api/v1/auth/logout` : Đăng xuất tài khoản.
- `POST /api/v1/auth/forgot-password/send-otp` : Gửi mã OTP khi quên mật khẩu.
- `POST /api/v1/auth/forgot-password/verify-otp` : Xác nhận mã OTP quên mật khẩu.
- `POST /api/v1/auth/forgot-password/reset-password` : Đặt lại mật khẩu mới.

**2. User Controller (`/api/v1/users`)**
- `GET /api/v1/users/test` : API kiểm tra trạng thái hoạt động của User Service.

## 3. Quy tắc dành cho AI Agent (Agent Rules)

1. **Kiến trúc Microservices**: Code thêm tính năng mới phải đặt vào đúng service nghiệp vụ (User, Dispatch, Resource,...).
2. **Spring Boot**: Sử dụng chuẩn của Spring Boot cho Controller, Service, Repository (Spring Data JPA).
3. **Thư viện dùng chung**: Tái sử dụng các class xử lý Exception (`GlobalExceptionHandler`) và định dạng Response trong `common-lib`.
4. **Cấu hình tập trung**: Cấu hình của các service được lưu tại `infrastructure/config-service/src/main/resources/configs`. Bất kỳ thay đổi config nào cần cập nhật vào đúng file yml tương ứng.
