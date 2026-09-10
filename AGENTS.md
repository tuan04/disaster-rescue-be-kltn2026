# Cấu trúc Dự án (Project Architecture) - Backend

Dự án này là hệ thống Backend phục vụ cho **Hệ thống Cứu hộ thảm họa (Disaster Rescue)**, được xây dựng theo kiến trúc Microservices với Java 17 / 21, Spring Boot 3, Spring Cloud, gRPC, Apache Kafka, Redis, PostgreSQL (PostGIS) và MongoDB.

---

## 1. Cấu trúc Tổng quan Hệ thống (System Architecture)

Hệ thống được chia thành hai nhóm chính: `core-service` (các business microservices) và `infrastructure` (các dịch vụ hạ tầng nền tảng).

```
disaster-rescue-be-kltn2026/
├── core-service/
│   ├── common-lib/                   # Thư viện dùng chung (Response, Exception, gRPC proto, Utility)
│   ├── user-service/                 # Quản lý người dùng, hồ sơ cứu hộ, xác thực JWT RSA & Redis
│   ├── dispatch-service/             # Điều phối cứu hộ, xử lý SOS, bản đồ PostGIS, điều động đội cứu hộ
│   ├── resource-management-service/  # Quản lý chiến dịch cứu hộ, đội cứu hộ, thành viên, kho & trang thiết bị
│   ├── notification-service/         # Gửi thông báo đẩy (Push Notification), WebSocket STOMP, log GPS vị trí
│   └── integration-service/          # Thu thập dữ liệu mạng xã hội (MongoDB), tiếp nhận phân tích AI
├── infrastructure/
│   ├── eureka-server/                # Service Registry & Discovery (Port 8761)
│   ├── config-service/               # Centralized Configuration Server (Port 8888)
│   └── api-gateway/                  # API Gateway trung tâm, xác thực JWT, định tuyến, CORS (Port 8000)
└── docker-compose.yaml               # Cấu hình môi trường hạ tầng (Kafka, Redis, Postgres PostGIS, Mongo)
```

---

## 2. Chi tiết Chức năng của từng Service

### A. Infrastructure Services

1. **`eureka-server` (Port 8761)**:
   - Đóng vai trò Service Registry / Service Discovery (Spring Cloud Netflix Eureka).
   - Cho phép các microservices đăng ký định danh và tự động phân giải địa chỉ khi giao tiếp nội bộ qua Gateway / LoadBalancer.

2. **`config-service` (Port 8888)**:
   - Máy chủ cấu hình tập trung (Spring Cloud Config Server).
   - Quản lý toàn bộ file cấu hình `.yml` cho tất cả các service tại thư mục `src/main/resources/configs/` (`api-gateway.yml`, `user-service.yml`, `dispatch-service.yml`, `resource-management-service.yml`, `notification-service.yml`, `integration-service.yml`).

3. **`api-gateway` (Port 8000)**:
   - Cổng giao tiếp API Gateway tập trung (Spring Cloud Gateway WebFlux).
   - **Xác thực JWT**: `JwtAuthFilter` giải mã Token qua cặp khóa bất đối xứng RSA, tự động trích xuất `userId` và gắn header `X-User-Id` vào request chuyển tiếp xuống các service nghiệp vụ.
   - **Định tuyến (Routing)**: Phân phối request đến đúng service theo path (`/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/sos-requests/**`, `/api/v1/map-points/**`, `/api/v1/assignments/**`, `/api/v1/campaigns/**`, `/api/v1/campaign-teams/**`, `/ws/**`).
   - **CORS & WebSocket Support**: Cấu hình CORS toàn cục và định tuyến kết nối WebSocket tới `notification-service`.

---

### B. Core Business Services

1. **`common-lib` (Thư viện dùng chung)**:
   - Chuẩn hóa cấu trúc phản hồi API: `ApiResponse<T>`, `ErrorResponse`.
   - Xử lý ngoại lệ toàn cục: `BusinessException`, `ErrorCode`, `GlobalExceptionHandler` (`@RestControllerAdvice`).
   - Giao tiếp gRPC: Định nghĩa protobuf contract `resource_service.proto`, `GrpcGlobalExceptionInterceptor`, `GrpcStatusMapper`.

2. **`user-service` (Quản lý người dùng & Xác thực - Port 8081)**:
   - **Xác thực & Phân quyền**: Đăng ký người dân (`CITIZEN`), đăng nhập, phân quyền (Role: `CITIZEN`, `RESCUER`, `MANAGER`, `ADMIN`).
   - **Cơ chế Token RSA & Redis**: Cấp phát Access Token (ngắn hạn) và Refresh Token (dài hạn) ký bằng khóa bất đối xứng RSA; cơ chế thu hồi token (Token Revocation/Blacklist) lưu trữ trên Redis.
   - **Xác thực OTP & Quên mật khẩu**: Sinh mã OTP và xác thực qua Redis để hỗ trợ đổi/quên mật khẩu.
   - **Nâng cấp tài khoản**: Tiếp nhận thông tin định danh CCCD để nâng cấp người dân thành Cứu hộ viên (`RESCUER`) với hồ sơ `VolunteerProfile`.

3. **`dispatch-service` (Điều phối cứu hộ & Bản đồ - Port 8083)**:
   - **Tiếp nhận & Quản lý SOS**: Người dân tạo và cập nhật yêu cầu cứu hộ khẩn cấp (`RescueRequest`).
   - **Xử lý Không gian PostGIS**: Sử dụng kiểu dữ liệu Geometry PostGIS (`Point`, `Polygon`, SRID 4326) để lưu tọa độ cứu hộ, xác định vị trí thuộc vùng quản lý/khu vực thảm họa (`Location`).
   - **Bản đồ Điểm cứu hộ (`MapPoint`)**: Tìm kiếm và hiển thị các điểm trên bản đồ đa dạng (Điểm SOS `PointType.SOS`, Vùng nguy hiểm `HAZARD`, Điểm an toàn `SAFE_ZONE`, Kho cứu trợ `WARE_HOUSE`) kèm bộ lọc đa tiêu chí (mức độ khẩn cấp, loại điểm, trạng thái, thời gian).
   - **Điều động đội cứu hộ (`Assignment`)**: Trưởng đội cứu hộ chấp nhận cứu hộ (`acceptRescueByLeader`). Service gọi **gRPC** sang `resource-management-service` để xác thực thông tin đội và tạo phân công nhiệm vụ.
   - **Phát sự kiện Kafka (`SosEventProducer`)**: Khi có yêu cầu SOS mới hoặc cập nhật trạng thái, tự động bắn sự kiện lên Kafka topic `sos-event`.

4. **`resource-management-service` (Quản lý Tài nguyên & Đội cứu hộ - Port 8082, gRPC 9092)**:
   - **Quản lý Chiến dịch (`Campaign`)**: Tạo và cập nhật thông tin các chiến dịch ứng phó thảm họa (trạng thái: `PLANNING`, `ACTIVE`, `PAUSED`, `COMPLETED`, `CANCELLED`).
   - **Quản lý Đội cứu hộ (`CampaignTeam`)**: Thiết lập đội cứu hộ tham gia chiến dịch, chỉ định Đội trưởng (`leaderId`, `leaderPhone`), phương tiện, trạng thái đội (`AVAILABLE`, `ASSIGNED`, `BUSY`, `INACTIVE`).
   - **Quản lý Thành viên Đội (`CampaignMember`)**: Thêm/xóa tình nguyện viên vào đội với các vai trò chuyên trách (`LEADER`, `MEMBER`, `DRIVER`, `MEDIC`, `LOGISTICS`).
   - **Quản lý Kho & Vật phẩm**: Cấu trúc dữ liệu theo dõi tồn kho cứu trợ (`Item`, `CampaignWarehouseInventory`, `TeamMobileInventory`).
   - **Cung cấp gRPC Server (`CampaignTeamGrpcService`)**: Lắng nghe tại port 9092, phục vụ RPC method `GetTeamByLeaderId` cho `dispatch-service` truy vấn thông tin đội cứu hộ qua leaderId với tốc độ cao.

5. **`notification-service` (Thông báo & Theo dõi Vị trí - Port 8084)**:
   - **Kafka Consumer (`SendPushNotification`)**: Lắng nghe topic `sos-event` từ Kafka để kích hoạt gửi thông báo đẩy khẩn cấp đến ứng dụng của các cứu hộ viên trong khu vực.
   - **WebSocket / STOMP Broker**: Cấu hình endpoint `/ws` (broker `/topic`, `/queue`, application prefix `/app`) phục vụ truyền tải dữ liệu thời gian thực (Real-time update) lên bản đồ và giao diện cứu hộ.
   - **Nhật ký Vị trí Đội cứu hộ (`TeamLocationLog`)**: Lưu vết lịch sử tọa độ GPS của đội cứu hộ (`geometry(Point, 4326)`) phục vụ giám sát hành trình di chuyển thực địa.

6. **`integration-service` (Tích hợp Nguồn tin & AI - Port 8085)**:
   - **Thu thập Dữ liệu MXH (`SocialRawFeed`)**: Lưu trữ tin tức, bài đăng kêu cứu thô từ các nền tảng mạng xã hội (Facebook, Twitter/X, Zalo,...) trên MongoDB.
   - **Kafka Consumer (`EvaluateSituation`)**: Lắng nghe sự kiện SOS từ Kafka topic `sos-event` để chuyển giao dữ liệu phục vụ AI đánh giá mức độ khẩn cấp và độ tin cậy của thông tin thảm họa.

---

## 3. Danh mục API Chi tiết (API Endpoints Reference)

Tất cả API công khai bên ngoài đều được gọi qua **API Gateway (`http://<gateway-host>:8000`)**.

### A. User Service (`/api/v1/auth`, `/api/v1/users`)

| Phương thức | Endpoint | Header / Auth | Mô tả chức năng | Request Body / Params |
|:---|:---|:---|:---|:---|
| `POST` | `/api/v1/auth/register` | Public | Đăng ký tài khoản người dân mới | `CitizenRegisterRequest` (phone, password, confirmPassword, fullName, birthDate, sex) |
| `POST` | `/api/v1/auth/verify-otp` | Public | Xác thực mã OTP sau đăng ký | `OtpVerificationRequest` (id, phoneNumber, otp) |
| `POST` | `/api/v1/auth/login` | Header `X-Client-Type: WEB\|MOBILE` | Đăng nhập (trả JWT qua Cookie/Body) | `LoginRequest` (phoneNumber, password) |
| `POST` | `/api/v1/auth/refresh-token` | Cookie hoặc `x-refresh-token` | Cấp lại Access Token mới | Refresh Token |
| `POST` | `/api/v1/auth/logout` | `Authorization: Bearer <token>` | Đăng xuất và đưa token vào Redis blacklist | Token & Cookie |
| `POST` | `/api/v1/auth/forgot-password/send-otp` | Public | Gửi mã OTP khôi phục mật khẩu qua SĐT | `ForgotPasswordRequest` (phoneNumber) |
| `POST` | `/api/v1/auth/forgot-password/verify-otp` | Public | Xác thực OTP quên mật khẩu, nhận Reset Token | `OtpVerificationRequest` (id, phoneNumber, otp) |
| `POST` | `/api/v1/auth/forgot-password/reset-password` | Header `reset-token: <token>` | Đặt lại mật khẩu mới | `ResetPasswordRequest` (phone, password, confirmPassword) |
| `GET` | `/api/v1/auth/user-info` | `Authorization: Bearer <token>` | Lấy thông tin cá nhân của người dùng hiện tại | Không |
| `PUT` | `/api/v1/users/upgrade-rescuer` | `Authorization: Bearer <token>` | Nâng cấp tài khoản Người dân lên Cứu hộ viên | `UpgradeRescuerRequest` (id, CCCD) |

---

### B. Dispatch Service (`/api/v1/sos-requests`, `/api/v1/map-points`, `/api/v1/assignments`)

| Phương thức | Endpoint | Header / Auth | Mô tả chức năng | Request Body / Params |
|:---|:---|:---|:---|:---|
| `POST` | `/api/v1/sos-requests` | Optional `Authorization` (`X-User-Id`) | Gửi yêu cầu cứu hộ khẩn cấp SOS (lưu tọa độ, bắn Kafka event) | `SOSRequest` (latitude, longitude, reporterPhone, content) |
| `PUT` | `/api/v1/sos-requests` | `Authorization: Bearer <token>` | Cập nhật thông tin / trạng thái yêu cầu SOS | `UpdateSOSRequest` (id, reporterPhone, content, status) |
| `GET` | `/api/v1/map-points` | Public / Rescuer | Lấy danh sách điểm bản đồ kèm bộ lọc (SOS, Hazard, Safe Zone, Warehouse) | Query Params: `pointTypes`, `rescueStatuses`, `emergencyLevels`, `hazardStatuses`, `fromTime`, `toTime`... |
| `GET` | `/api/v1/map-points/{id}` | Public / Rescuer | Xem chi tiết thông tin một điểm trên bản đồ theo ID | Path Variable: `id` (UUID) |
| `POST` | `/api/v1/assignments/rescue-requests/{requestId}/accept` | `Authorization: Bearer <token>` | Trưởng đội cứu hộ chấp nhận yêu cầu cứu nạn (gọi gRPC xác thực đội) | Path: `requestId`<br>Query: `leaderId`, `note` |

---

### C. Resource Management Service (`/api/v1/campaigns`, `/api/v1/campaign-teams`, `/api/v1/teams`, `/api/v1/campaign-warehouse-inventories`, `/api/v1/items`)

| Phương thức | Endpoint | Header / Auth | Mô tả chức năng | Request Body / Params |
|:---|:---|:---|:---|:---|
| `POST` | `/api/v1/campaigns` | `Authorization` (Manager/Admin) | Tạo mới chiến dịch cứu hộ thảm họa | `CreateCampaignRequest` (name, startDate) |
| `PATCH` | `/api/v1/campaigns/{id}` | `Authorization` (Manager/Admin) | Cập nhật thông tin / trạng thái chiến dịch | Path: `id`<br>`UpdateCampaignRequest` (name, startDate, endDate, status) |
| `POST` | `/api/v1/campaign-teams` | `Authorization` (Manager/Admin) | Thành lập đội cứu hộ mới cho chiến dịch | `CreateTeamRequest` (campaignId, teamName, leaderId, leaderPhone) |
| `PATCH` | `/api/v1/campaign-teams/{id}` | `Authorization` (Manager/Admin) | Cập nhật thông tin đội cứu hộ (phương tiện, trạng thái, sđt) | Path: `id`<br>`UpdateTeamRequest` (totalParticipants, status, vehicles, teamName, leaderId, leaderPhone) |
| `POST` | `/api/v1/teams/{teamId}/members` | `Authorization` (Manager/Leader) | Thêm thành viên vào đội cứu hộ theo vai trò | Path: `teamId`<br>`AddMemberRequest` (memberId, roleInTeam) |
| `DELETE` | `/api/v1/teams/{teamId}/members/{memberId}` | `Authorization` (Manager/Leader) | Xóa thành viên khỏi đội cứu hộ | Path: `teamId`, `memberId` |
| `POST` | `/api/v1/campaign-warehouse-inventories` | `Authorization` (Manager/Admin) | Thêm vật phẩm vào kho của chiến dịch | `CreateCampaignWarehouseInventoryRequest` (campaignId, warehouseId, itemId, quantity) |
| `PATCH` | `/api/v1/campaign-warehouse-inventories/{id}` | `Authorization` (Manager/Admin) | Cập nhật thông tin tồn kho (số lượng, kho) | Path: `id`<br>`UpdateCampaignWarehouseInventoryRequest` (quantity, warehouseId) |
| `DELETE` | `/api/v1/campaign-warehouse-inventories/{id}` | `Authorization` (Manager/Admin) | Xóa mềm tồn kho chiến dịch (đặt `isDeleted = true`) | Path: `id` |
| `GET` | `/api/v1/campaign-warehouse-inventories/{id}` | Public / Authenticated | Xem chi tiết tồn kho theo ID | Path: `id` |
| `GET` | `/api/v1/campaign-warehouse-inventories` | Public / Authenticated | Lấy danh sách tồn kho (lọc theo campaignId, warehouseId) | Query Params: `campaignId`, `warehouseId` |
| `POST` | `/api/v1/items` | `Authorization` (Manager/Admin) | Thêm mới vật phẩm (hỗ trợ JSON hoặc multipart/form-data upload ảnh lên S3) | JSON: `CreateItemRequest`<br>hoặc FormData: `name`, `unit`, `imageUrl`, `image` |
| `PATCH` | `/api/v1/items/{id}` | `Authorization` (Manager/Admin) | Cập nhật thông tin vật phẩm (hỗ trợ JSON hoặc multipart) | Path: `id`<br>JSON: `UpdateItemRequest`<br>hoặc FormData: `name`, `unit`, `imageUrl`, `image` |
| `DELETE` | `/api/v1/items/{id}` | `Authorization` (Manager/Admin) | Xóa mềm vật phẩm (đặt `isDeleted = true`) | Path: `id` |
| `GET` | `/api/v1/items/{id}` | Public / Authenticated | Xem chi tiết một vật phẩm theo ID | Path: `id` |

---

### D. gRPC Service Contract (`ResourceTeamGrpcService`)

Được định nghĩa tại `core-service/common-lib/src/main/proto/resource_service.proto`:

```protobuf
syntax = "proto3";
package iuh.fit.common.grpc;

service ResourceTeamGrpcService {
    rpc GetTeamByLeaderId (GetTeamByLeaderRequest) returns (TeamInfoResponse);
}
```

- **Server**: `resource-management-service` (Port gRPC: 9092, implement tại `CampaignTeamGrpcService`).
- **Client**: `dispatch-service` (Gọi qua `ResourceTeamGrpcClient` khi thực hiện phân công nhiệm vụ cứu hộ).

---

### E. Giao tiếp Thời gian thực & Hàng đợi Sự kiện (Kafka & WebSocket)

| Giao thức | Tên Kênh / Topic | Service Sản xuất (Producer) | Service Tiêu thụ (Consumer) | Mục đích |
|:---|:---|:---|:---|:---|
| **Kafka** | `sos-event` | `dispatch-service` | `notification-service`, `integration-service` | Thông báo sự kiện SOS tức thì và kích hoạt AI phân tích |
| **WebSocket** | `/ws` (STOMP) | `notification-service` | Web / Mobile Frontend Clients | Đẩy dữ liệu cập nhật tọa độ cứu hộ thời gian thực (`/topic`, `/queue`) |

---

## 4. Cơ sở Dữ liệu & Công nghệ Sử dụng

| Service | Cơ sở dữ liệu / Công nghệ lưu trữ | Mục đích chính |
|:---|:---|:---|
| `user-service` | PostgreSQL + Redis | Lưu trữ tài khoản người dùng, hồ sơ cứu hộ; Redis lưu OTP và JWT Blacklist |
| `dispatch-service` | PostgreSQL + **PostGIS** | Lưu trữ không gian bản đồ (Điểm SOS, vùng nguy hiểm, điểm an toàn, ranh giới khu vực) |
| `resource-management-service` | PostgreSQL | Lưu trữ chiến dịch, cơ cấu đội cứu hộ, thành viên, kho vật tư |
| `notification-service` | PostgreSQL + **PostGIS** | Lưu lịch sử log tọa độ GPS di chuyển của các đội cứu hộ (`TeamLocationLog`) |
| `integration-service` | MongoDB | Lưu trữ dữ liệu phi cấu trúc bài đăng MXH (`SocialRawFeed`) phục vụ trích xuất AI |

---

## 5. Quy tắc dành cho AI Agent (Agent Rules)

1. **Tuân thủ Kiến trúc Microservices**:
   - Khi phát triển tính năng mới, phải đặt đúng vào service chịu trách nhiệm nghiệp vụ tương ứng (User, Dispatch, Resource, Notification, Integration).
   - Tuyệt đối không query chéo Database giữa các service; sử dụng **gRPC** cho giao tiếp đồng bộ hiệu năng cao nội bộ và **Kafka** cho luồng sự kiện bất đồng bộ.
2. **Chuẩn mã nguồn Spring Boot & Java**:
   - Sử dụng mô hình chuẩn 3 lớp: `Controller` -> `Service` / `ServiceImpl` -> `Repository` (Spring Data JPA / MongoRepository).
   - Áp dụng Bean Validation (`@Valid`, `@NotNull`, `@Pattern`,...) và DTO record/class cho toàn bộ Request/Response.
3. **Thư viện dùng chung (`common-lib`)**:
   - Mọi API trả về phải bọc trong `ApiResponse<T>`.
   - Ném ngoại lệ nghiệp vụ thông qua `BusinessException(ErrorCode.XYZ, message)` để `GlobalExceptionHandler` bắt và format chuẩn.
4. **Quản lý Cấu hình Tập trung**:
   - Toàn bộ cấu hình `application.yml` của các microservices được quản lý tại `infrastructure/config-service/src/main/resources/configs/`. Bất kỳ thay đổi biến môi trường hay config cổng, database phải cập nhật vào file YAML tương ứng tại đây.
